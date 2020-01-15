/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package samples

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, MediaRanges }
import akka.stream.alpakka.csv.scaladsl.{ CsvParsing, CsvToMap }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.ByteString
import spray.json.{ DefaultJsonProtocol, JsValue, JsonWriter }

import scala.concurrent.Future

object Main
  extends App
    with DefaultJsonProtocol {

  implicit val actorSystem: ActorSystem[Nothing] = ActorSystem[Nothing](Behaviors.empty, "alpakka-samples")

  import actorSystem.executionContext

  val httpRequest = HttpRequest(uri = "https://www.nasdaq.com/screening/companies-by-name.aspx?exchange=NASDAQ&render=download")
    .withHeaders(Accept(MediaRanges.`text/*`))

  def extractEntityData(response: HttpResponse): Source[ByteString, _] =
    response match {
      case HttpResponse(OK, _, entity, _) => entity.dataBytes
      case notOkResponse =>
        Source.failed(new RuntimeException(s"illegal response $notOkResponse"))
    }

  def cleanseCsvData(csvData: Map[String, ByteString]): Map[String, String] =
    csvData
      .filterNot { case (key, _) => key.isEmpty }
      .view
      .mapValues(_.utf8String)
      .toMap

  def toJson(map: Map[String, String])(
    implicit jsWriter: JsonWriter[Map[String, String]]): JsValue = jsWriter.write(map)

  val future: Future[Done] =
    Source
      .single(httpRequest) //: HttpRequest
      .mapAsync(1)(Http()(actorSystem.toClassic).singleRequest(_)) //: HttpResponse
      .flatMapConcat(extractEntityData) //: ByteString
      .via(CsvParsing.lineScanner()) //: List[ByteString]
      .via(CsvToMap.toMap()) //: Map[String, ByteString]
      .map(cleanseCsvData) //: Map[String, String]
      .map(toJson) //: JsValue
      .map(_.compactPrint) //: String (JSON formatted)
      .runWith(Sink.foreach(println))

  val cs: CoordinatedShutdown = CoordinatedShutdown(actorSystem)
  cs.addTask(CoordinatedShutdown.PhaseServiceStop, "shut-down-client-http-pool")( () =>
    Http()(actorSystem.toClassic).shutdownAllConnectionPools().map(_ => Done)
  )

  future.onComplete { _ =>
    println("Done!")
    cs.run(CoordinatedShutdown.UnknownReason)
  }

}

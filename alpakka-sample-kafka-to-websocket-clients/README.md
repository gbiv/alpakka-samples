# Alpakka sample

## Read from a Kafka topic and push the data to connected websocket clients

Clients may connect via websockets and will receive data read from a Kafka topic. The websockets are implemented in @extref[Akka HTTP](akka-http:) and [Alpakka Kafka](alpakka-kafka:) subscribes to the Kafka topic.

Browse the sources at @link:[Github](https://github.com/akka/alpakka-samples/tree/master/alpakka-sample-kafka-to-websocket-clients) { open=new }.

To try out this project clone @link:[the Alpakka Samples repository](https://github.com/akka/alpakka-samples) { open=new } and find it in the `alpakka-sample-kafka-to-websocket-clients` directory.

## Running

The sample spawns a test Kafka server with docker. 

```
sbt "runMain samples.javadsl.Main"
```

You can connect to ws://127.0.0.1/events to receive messages over websockets. E.g. Using [`websocat`](https://github.com/vi/websocat) as a simple WS client.

To listen to events coming in on the websocket use `websocat` to connect to the `/events` endpoint.

```
websocat -v ws://127.0.0.1:8081/events 
```

You can use `curl` to post messages to the topic.

```
curl http://127.0.0.1:8081/push?value=message
```

## Run with datadog tracing
### Setup the agent via Docker
Create a script to help run the agent locally with Docker similar to the following:
```bash 
#/bin/sh
DOCKER_CONTENT_TRUST=1 docker run -d --name dd-agent -p 8126:8126/tcp \
-v /var/run/docker.sock:/var/run/docker.sock:ro \
-v /proc/:/host/proc/:ro \
-v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro \
-e DD_HOSTNAME=<your desired hostname mapping> \
-e DD_API_KEY=<your DD API KEY>\
-e DD_APM_ENABLED=true \
-e DD_APM_NON_LOCAL_TRAFFIC=true \
-e DD_LOGS_ENABLED=true \
-e DD_CONTAINER_EXCLUDE="name:dd-agent" \
-e DD_APM_ENV=local \
-e DD_ENV=local \
-e DD_LOG_LEVEL=info \
-l com.datadoghq.ad.init_configs='["{}"]' \
datadog/agent:latest
```  

See the official [Datadog Docker Agent docs](https://docs.datadoghq.com/agent/docker/?tab=standard) if you run into trouble.

---

### Setup the environment for running the application
Set these environment variables
```bash
DD_SERVICE=<your service name>;
DD_ENV=local; // make sure this matches the APM_ENV and ENV specified in the agent
DD_TRACE_ENABLED=true;
DD_LOGS_INJECTION=true;
DD_AGENT_HOST=localhost;
DD_TRACE_SAMPLE_RATE=1;
DD_TRACE_ANALYTICS_ENABLED=true
```

Get the dd tracing agent and set it for the jvm
- Agent jar from [maven](https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/0.95.1/)
- Specify the path to the tracing agent when starting the app.  
  ```-javaagent:/<path/to/dd-java-agent{version}.jar> ```

Once started you should see output in the log showing the configuration of the tracer. If you don't see a line like this, it's not working.
```bash
[dd.trace 2022-02-17 12:56:16:795 -0600] [dd-task-scheduler] INFO datadog.trace.agent.core.StatusLogger - DATADOG TRACER CONFIGURATION {"version":"0.95.1~93057308ae", ...}
```

## Viewing traces
Now, start the agent using the script and run the application as described above. 
Go the APM page in Datadog and filter traces by `env:<DD_ENV>`

Send some request to the `/push` endpoint and view the traces.


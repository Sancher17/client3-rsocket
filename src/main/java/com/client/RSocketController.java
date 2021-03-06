package com.client;

import com.client.data.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class RSocketController {

    static final String SERVER = "Server";
    static final String RESPONSE = "Response";
    static final String STREAM = "Stream";
    static final String CHANNEL = "Channel";

    public RSocketRequester rSocketRequester;

    private final List<RSocketRequester> CLIENTS = new ArrayList<>();

    @PreDestroy
    void shutdown() {
        log.info("Detaching all remaining clients...");
        CLIENTS.stream().forEach(requester -> requester.rsocket().dispose());
        log.info("Shutting down.");
    }

    public String checkConnect(RSocketRequester requester) {
        // Callback to client, confirming connection
        requester.route("client-test")
                .data("OPEN")
                .retrieveMono(String.class)
//                .retrieveFlux(String.class)
                .doOnNext(s -> log.info("Alex get answer: {}.", s))
                .subscribe();
        return "OK 2";
    }

    @ConnectMapping("shell-client")
    public void connectShellClientAndAskForTelemetry(RSocketRequester requester, @Payload String client) {
        this.rSocketRequester = requester;
        requester.rsocket()
                .onClose()
                .doFirst(() -> {
                    // Add all new clients to a client list
                    log.info("Client: {} CONNECTED.", client);
                    CLIENTS.add(requester);
                })
                .doOnError(error -> {
                    // Warn when channels are closed by clients
                    log.warn("Channel to client {} CLOSED", client);
                })
                .doFinally(consumer -> {
                    // Remove disconnected clients from the client list
                    CLIENTS.remove(requester);
                    log.info("Client {} DISCONNECTED", client);
                })
                .subscribe();

        // Callback to client, confirming connection
//        requester.route("client-status")
//                .data("OPEN")
//                .retrieveFlux(String.class)
//                .doOnNext(s -> log.info("Client: {} Free Memory: {}.", client, s))
//                .subscribe();
    }

    @MessageMapping("request-response")
    public Mono<Message> requestResponse(final Message request) {
        log.info("Received request-response request: {}", request);
        // create a single Message and return it
        return Mono.just(new Message(SERVER, RESPONSE));
    }

    @MessageMapping("fire-and-forget")
    public Mono<Void> fireAndForget(final Message request) {
        log.info("Received fire-and-forget request: {}", request);
        return Mono.empty();
    }

    @MessageMapping("stream")
    public Flux<Message> stream(final Message request) {
        log.info("Received stream request: {}", request);
        return Flux
                // create a new indexed Flux emitting one element every second
                .interval(Duration.ofSeconds(1))
                // create a Flux of new Messages using the indexed Flux
                .map(index -> new Message(SERVER, STREAM, index));
    }

    @MessageMapping("channel")
    public Flux<Message> channel(final Flux<Duration> settings) {
        log.info("Received channel request...");
        return settings
                .doOnNext(setting -> log.info("Channel frequency setting is {} second(s).", setting.getSeconds()))
                .doOnCancel(() -> log.warn("The client cancelled the channel."))
                .switchMap(setting -> Flux.interval(setting)
                        .map(index -> new Message(SERVER, CHANNEL, index)));
    }
}

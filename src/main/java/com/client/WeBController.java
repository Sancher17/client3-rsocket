package com.client;

import io.rsocket.RSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping(value = "/")
@RestController
public class WeBController {

    @Autowired
    RSocketController rSocketController;


    @GetMapping
    public String test(){
        log.info("REST Controller, test called");
        rSocketController.checkConnect(rSocketController.rSocketRequester);
//        requester.route("client-status")
//                .data("OPEN")
//                .retrieveFlux(String.class)
//                .doOnNext(s -> log.info("Free Memory AlEX: {}.", s))
//                .subscribe();
        return "OK 2";
    }
}

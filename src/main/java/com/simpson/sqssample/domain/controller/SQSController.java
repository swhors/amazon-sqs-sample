package com.simpson.sqssample.domain.controller;

import com.simpson.sqssample.domain.service.SNSPushMsgService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/sqs")
public class SQSController {
    @Autowired
    private SNSPushMsgService snsPushMsgService;
    
    @Getter
    @AllArgsConstructor
    public class SqsMessage {
        private String name;
        private LocalDate executeDate;
    }
    
    @GetMapping("/send")
    public String send(@RequestBody String message){
        String[] items = message.split("::");
        if (items.length != 7 ) return "error";
        snsPushMsgService.send(message,
                               items[2],
                               Integer.parseInt(items[3]),
                               Long.parseLong(items[6]));
        return "success";
    }
}

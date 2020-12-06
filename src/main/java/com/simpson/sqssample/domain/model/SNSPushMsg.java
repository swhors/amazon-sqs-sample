package com.simpson.sqssample.domain.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SNSPushMsg {
    private String cmdType;
    private String senderIdx;
    private String targetIdx;
    private int targetType;
    private String receivers;
    private long msgIdx;
    
    @Builder
    public SNSPushMsg(String cmdType,
                      String senderIdx,
                      String targetIdx,
                      int targetType,
                      List<String> receivers,
                      long msgIdx) {
        this.cmdType = cmdType;
        this.senderIdx = senderIdx;
        this.targetIdx = targetIdx;
        this.targetType = targetType;
        this.receivers = String.join(",", receivers);
        this.msgIdx = msgIdx;
    }
    
    public String toJSON() {
        return "{\n" +
                "  \"cmdType\":\""+ cmdType + "\",\n" +
                "  \"senderIdx\":\""+ senderIdx + "\",\n" +
                "  \"targetIdx\":\""+ targetIdx + "\",\n" +
                "  \"targetType\":\""+ targetType + "\",\n" +
                "  \"receiverList\":\""+ receivers + "\",\n" +
                "  \"msgIdx\":\""+ msgIdx + "\"\n" +
                "}";
    }
}

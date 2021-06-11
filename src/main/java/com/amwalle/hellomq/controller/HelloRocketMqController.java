package com.amwalle.hellomq.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.amwalle.hellomq.dto.ResponseDTO;
import com.amwalle.hellomq.mq.ProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@RestController
public class HelloRocketMqController {
    private final static Logger logger = LoggerFactory.getLogger(HelloRocketMqController.class);

    @RequestMapping(method = RequestMethod.POST, path = "/send-message")
    public void sendMessage(HttpServletRequest request, HttpServletResponse response) {
        Object data = getRequestData(request);
        JSONObject input = JSON.parseObject(data.toString());
        boolean sendStatus = ProducerService.sendMessage(input.getString("topic"), input.getString("tags"), input.getString("message"));
        if (!sendStatus) {
            sendJSONPResponse(response, new ResponseDTO("100", "failed to send message"));
        } else {
            sendJSONPResponse(response, new ResponseDTO("200", "success"));
        }
    }

    public Object getRequestData(HttpServletRequest request) {
        Object data = null;
        try {
            InputStream inputStream = request.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line);
            }
            data = JSON.parse(stringBuffer.toString());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return data;
    }

    public void sendJSONPResponse(HttpServletResponse response, Object content) {
        try {
            String result = JSON.toJSONString(content);
            response.setHeader("Content-Type", "application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(result);
        } catch (Exception e) {
            logger.error("Send JSONP response error", e);
        }
    }
}

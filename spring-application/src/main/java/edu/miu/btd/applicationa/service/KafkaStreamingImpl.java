package edu.miu.btd.applicationa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaStreamingImpl implements KafkaStreaming{

    private final KafkaTemplate<String, String> kafkaTemplate;
    @Override
    public void streamData(String data) {
        try {
            kafkaTemplate.send("application-a", data);
            log.info("=====> voting publish: {}", data);
        } catch (Exception ex) {
            log.info("streaming data error: ", ex);
        }
    }
}

package edu.miu.btd.applicationa.scheduler;

import edu.miu.btd.applicationa.service.KafkaStreaming;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@EnableAsync
@Component
@RequiredArgsConstructor
public class VotingScheduling {

    private final KafkaStreaming kafkaStreaming;

    private static final List<String> OS = List.of("Windows", "Linux", "Android", "IOS", "Mac OS");

    @Async
    @Scheduled(fixedDelay = 15000)
    public void votingSimulator() {
        Random rand = new Random();
        int randomIndex = rand.nextInt(OS.size());
        kafkaStreaming.streamData(OS.get(randomIndex));
    }
}

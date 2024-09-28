package edu.miu.btd;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;

import java.util.*;
import java.util.logging.Logger;

public class App {

    static final Map<String, Long> inMemoryData = new HashMap<>();

    static {
        inMemoryData.put("Windows", 0L);
        inMemoryData.put("Linux", 0L);
        inMemoryData.put("Android", 0L);
        inMemoryData.put("IOS", 0L);
        inMemoryData.put("Mac OS", 0L);
    }

    ;

    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger(String.valueOf(App.class));

        String topics = "application-a";

        SparkConf sparkConf = new SparkConf().setAppName("JavaDirectKafkaStreaming");
        sparkConf.setMaster("local[*]");
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(2));

        Set<String> topicsSet = new HashSet<>(Arrays.asList(topics.split(",")));

        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, "spark-streaming");
        kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JavaInputDStream<ConsumerRecord<String, String>> messages = KafkaUtils.createDirectStream(
                jssc,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(topicsSet, kafkaParams));

        JavaDStream<String> lines = messages.map(ConsumerRecord::value);

        lines.foreachRDD(x -> {
            List<String> strings = x.collect();
            if (strings.size() > 0) {
                String streamValue = strings.stream().findFirst().get();
                logger.info("=====> data..." + streamValue);
                long votes = 0;
                if (inMemoryData.containsKey(streamValue)) {
                    votes = inMemoryData.get(streamValue) + 1L;
                    inMemoryData.put(streamValue, votes);
                }

                logger.info("========= Latest Voting Result =========");
                for (Map.Entry<String, Long> entry : inMemoryData.entrySet()) {
                    logger.info(String.format("Candidate: %s, Votes: %s", entry.getKey(), entry.getValue()));
                }

                Configuration conf = HBaseConfiguration.create();
                conf.set("hbase.zookeeper.quorum", "zoo");
                conf.set("hbase.zookeeper.property.clientPort", "2181");
                Connection connection = ConnectionFactory.createConnection(conf);

                // track event of voting for audit log
                Table table = connection.getTable(TableName.valueOf("voting"));
                Put hdata = new Put(UUID.randomUUID().toString().getBytes());
                hdata.addColumn("cf".getBytes(), "candidate".getBytes(), streamValue.getBytes());
                hdata.addColumn("cf".getBytes(), "votes".getBytes(), String.valueOf(votes).getBytes());
                table.put(hdata);
                table.close();

                // store only final result
                table = connection.getTable(TableName.valueOf("voting_result"));
                Get get = new Get(streamValue.getBytes());
                Result result = table.get(get);

                if (!result.isEmpty()) {
                    // Extract data
                    byte[] value = result.getValue("cf".getBytes(), "total_votes".getBytes());
                    long totalVote = Long.parseLong(new String(value));
                    totalVote = totalVote + 1L;

                    // Update data
                    Put put = new Put(streamValue.getBytes());
                    put.addColumn("cf".getBytes(), "total_votes".getBytes(), String.valueOf(totalVote).getBytes());
                    table.put(put);
                    table.close();
                } else {
                    table = connection.getTable(TableName.valueOf("voting_result"));
                    Put votingResultData = new Put(streamValue.getBytes());
                    votingResultData.addColumn("cf".getBytes(), "total_votes".getBytes(), String.valueOf(votes).getBytes());
                    table.put(votingResultData);
                    table.close();
                }
                connection.close();
            }
        });

        // Start the computation
        jssc.start();
        jssc.awaitTermination();
    }
}

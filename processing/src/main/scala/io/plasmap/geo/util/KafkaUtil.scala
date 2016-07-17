package io.plasmap.geo.util

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.{ConsumerSettings, ProducerSettings}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer}

object KafkaUtil {

  /*def createSinkProperties[T](host: String, topic: String, clientId: String, encoder: Encoder[T], partitionizer: (T) => Option[Array[Byte]]): ProducerProperties[T] =
    ProducerProperties(host, topic, clientId, encoder, partitionizer)
      .asynchronous()
      .noCompression()*/

  val bytesToProducerRecord: (String) => (Array[Byte]) => ProducerRecord[Array[Byte], Array[Byte]] =
    topic => elem => new ProducerRecord[Array[Byte], Array[Byte]](topic, elem)

  val consumerRecordToBytes: (ConsumerRecord[Array[Byte],Array[Byte]]) => Array[Byte] =
    _.value()

  def kafkaSink(host: String)(implicit system: ActorSystem): Sink[ProducerRecord[Array[Byte], Array[Byte]], NotUsed] = {
    val producerSettings =
      ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer)
        .withBootstrapServers(host)
        .withParallelism(4)

    Producer.plainSink(producerSettings)
  }

  def kafkaSource(host:String)(topic:String,group:String)(implicit system:ActorSystem): Source[ConsumerRecord[Array[Byte], Array[Byte]], Control] = {

    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new ByteArrayDeserializer,
      Set(topic))
      .withBootstrapServers(host)
      .withGroupId(group)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    Consumer.plainSource(consumerSettings)
  }
}

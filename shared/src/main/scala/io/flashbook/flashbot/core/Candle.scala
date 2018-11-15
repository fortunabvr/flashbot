package io.flashbook.flashbot.core

import io.flashbook.flashbot.core.MarketData.GenMD
import io.flashbook.flashbot.util.parseProductId

case class Candle(micros: Long,
                  open: Double,
                  high: Double,
                  low: Double,
                  close: Double,
                  volume: Option[Double] = None) extends Timestamped {
  def add(value: Double, newVolume: Option[Double] = None): Candle = copy(
    high = math.max(high, value),
    low = math.min(low, value),
    close = value,
    volume = newVolume.orElse(volume)
  )
}

object Candle {
  import io.circe._
  import io.circe.generic.semiauto._

  implicit val candleEn: Encoder[Candle] = deriveEncoder
  implicit val candleDe: Decoder[Candle] = deriveDecoder

  case class CandleMD(source: String, topic: String, data: Candle)
    extends GenMD[Candle] with Priced {

    override def micros: Long = data.micros
    override def dataType: String = "candles"
    override def exchange: String = source
    override def product: Pair = parseProductId(topic)
    override def price: Double = data.close
  }
}
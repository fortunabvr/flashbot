package strategies

import core.DataSource.DataSourceConfig
import core.{Account, MarketData, Strategy, TradingSession}
import io.circe.Json

/**
  * Do you have a magical feed of buy/sell signals? This strategy simply turns those signals into
  * orders on an exchange as quickly as possible.
  */
class ExternalSignal extends Strategy {
  override def title: String = "External Signal"

  override def initialize(jsonParams: Json,
                          dataSourceConfig: Map[String, DataSourceConfig],
                          initialBalances: Map[Account, Double]): List[String] = ???

  override def handleData(data: MarketData)(implicit ctx: TradingSession): Unit = ???
}

package exchanges

import core.AggBook.{AggBook, AggBookMD}
import core.OrderBook.OrderBookMD
import core._

import scala.collection.immutable.Queue

/**
  * The simulator is an exchange used for backtesting and paper trading. It takes an instance of a
  * real exchange as a parameter to use as a base implementation, but it simulates all API
  * interactions so that no network requests are actually made.
  */
class Simulator(base: Exchange, latencyMicros: Long) extends Exchange {

  private var currentTimeMicros: Long = 0

  sealed trait APIRequest {
    def requestTime: Long
  }
  case class OrderReq(requestTime: Long, req: OrderRequest) extends APIRequest
  case class CancelReq(requestTime: Long, id: String) extends APIRequest

  private var apiRequestQueue = Queue.empty[APIRequest]

  private var myOrders = Map.empty[Pair, OrderBook]

  private var depths = Map.empty[Pair, AggBook]
  private var prices = Map.empty[Pair, Double]

  override def makerFee: Percent = base.makerFee
  override def takerFee: Percent = base.takerFee
  override def formatPair(pair: Pair): String = base.formatPair(pair)

  override def update(session: TradingSession,
                      data: MarketData): (Seq[Order.Fill], Seq[OrderEvent]) = {
    var fills = Seq.empty[Order.Fill]
    var events = Seq.empty[OrderEvent]

    // Dequeue and process API requests that have passed the latency threshold
    while (apiRequestQueue.headOption.exists(_.requestTime + latencyMicros < data.micros)) {
      apiRequestQueue.dequeue match {
        case (r: APIRequest, rest) =>
          r match {
            case OrderReq(requestTime, req) => req match {
              /**
                * Limit orders may be filled (fully or partially) immediately. If not immediately
                * fully filled, the remainder is placed on the resting order book.
                */
              case LimitOrderRequest(clientOid, side, product, price, size) =>

              /**
                * Market orders need to be filled immediately.
                */
              case MarketOrderRequest(clientOid, side, product, size, funds) =>
            }

            /**
              * Removes the identified order from the resting order book.
              */
            case CancelReq(_, id) =>
              myOrders.foreach {
                case (product, book) if book.orders.contains(id) =>
                  val order = book.orders(id)
                  myOrders = myOrders + (product -> book.done(id))
                  events = events :+ Done(id, product, order.side, Canceled,
                    order.price, Some(order.amount))
                case _ =>
              }
          }
          apiRequestQueue = rest
      }
    }

    // Update the current time
    currentTimeMicros = data.micros

    // Update latest depth/pricing data
    data match {
      case md: OrderBookMD[_] =>
        // TODO: Turn aggregate full order books into aggregate depths here
        ???
      case md: AggBookMD =>
        depths = depths + (md.product -> md.data)
      case md: TradeMD =>
        prices = prices + (md.product -> md.data.price)
    }

    (fills, events)
  }

  override def order(req: OrderRequest): Unit = {
    apiRequestQueue = apiRequestQueue.enqueue(OrderReq(currentTimeMicros, req))
  }

  override def cancel(id: String): Unit = {
    apiRequestQueue = apiRequestQueue.enqueue(CancelReq(currentTimeMicros, id))
  }
}
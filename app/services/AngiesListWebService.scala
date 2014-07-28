package services

import java.io.IOException

import play.api.libs.json._
import play.api.libs.ws.WS

import scala.concurrent.{Future, ExecutionContext}

case class MalformedJsonResponse(statusCode: Int, jsError: JsError) extends IOException(s"$statusCode: $jsError")

case class WebServiceErrorResponse[E: Reads](statusCode: Int, body: E) extends IOException(s"$statusCode: $body")

object AngiesListWebService extends ResponseCodes {
  def post[R: Writes, S: Reads, E: Reads](url: String, body: R, bodyToLog: Option[R] = None)(implicit executor: ExecutionContext): Future[S] = {
    val jsonBody = Json.toJson(body)
    val jsonBodyToLog = Json.toJson(bodyToLog.getOrElse(body))

    WS.url(url) post jsonBody flatMap {
      response =>
        response.status match {
          case Success(code) => Json.fromJson[S](response.json) match {
            case JsSuccess(value, _) => Future.successful(value)
            case jsError: JsError => {
              Future.failed(MalformedJsonResponse(code, jsError))
            }
          }
          case code => Json.fromJson[E](response.json) match {
            case JsSuccess(value, _) => {
              Future.failed(WebServiceErrorResponse(code, value)) // This is the case in which our bug occurs. This
              // failed future is logged by the New Relic agent despite the fact that it is recovered in Application.scala
            }
            case jsError: JsError => {
              Future.failed(MalformedJsonResponse(code, jsError))
            }
          }
        }
    }
  }
}

trait ResponseCodes {

  class ResponseCodeRange(range: Range) {
    def unapply(status: Int): Option[Int] = Some(status) filter (range contains _)
  }

  object Success extends ResponseCodeRange(200 until 300)

}
package services

import java.io.IOException

import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

object UserInfoService {
  val url = "http://localhost:9000/remoteService"
  def getSuperImportantUserInfo(userId: Int)(implicit executor: ExecutionContext): Future[UserInfoResponse] = {
    enrich(AngiesListWebService.post[UserInfoRequest, UserInfoResponse, UserInfoErrorResponse](url, UserInfoRequest(userId)))
  }

  def enrich[A](futureResponse: Future[A])(implicit executor: ExecutionContext) =
    futureResponse.transform(identity, enrichFailure)

  private def enrichFailure(failure: Throwable): Throwable = failure match {
    case WebServiceErrorResponse(statusCode, error: UserInfoErrorResponse) =>
      UserInfoResponseException(statusCode, error)
    case _ => failure
  }

  case class UserInfoRequest(userId: Int)
  case class UserInfoResponse(message: String)
  case class UserInfoErrorResponse(message: String)
  case class UserInfoResponseException(statusCode: Int, errorResponse: UserInfoErrorResponse)
    extends IOException(errorResponse.message)

  implicit val requestFormat = Json.format[UserInfoRequest]
  implicit val responseFormat = Json.format[UserInfoResponse]
  implicit val errorFormat = Json.format[UserInfoErrorResponse]
}


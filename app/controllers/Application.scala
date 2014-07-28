package controllers

import play.api.libs.json.Json
import play.api.mvc._
import services.UserInfoService
import services.UserInfoService._
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action {
    val userId = 12345
    val superImportantUserInfo = UserInfoService.getSuperImportantUserInfo(userId)

    Async {
      superImportantUserInfo.map { response =>
        "Yeah! We've got your info." // This happens when the remote service returns 200.
      } recover {
        case error: UserInfoResponseException =>
          "We couldn't find your info. Oh well." // This is where our bug occurs.
        case _ =>
          "Something actually went wrong." // This represents an actual unexpected Exception.
      } map { case (message) =>
        Ok(message)
      }
    }
  }

  def remoteService = Action {
    request =>
      BadRequest(Json.obj("message" -> "No information found."))
  }
}
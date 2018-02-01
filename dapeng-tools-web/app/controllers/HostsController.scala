package controllers

import javax.inject._

import com.github.dapeng.tools.helpers.{RouteInfoHelper, RequestExampleHelper, MetaInfoHelper}
import module.ServiceInfo
import play.api.mvc._
import util.ZookeeperHelper

import scala.collection.mutable.ListBuffer

@Singleton
class HostsController @Inject() extends Controller {


  def listHosts = Action {
    val serviceInfos = ZookeeperHelper.getInfos()
    Ok(views.html.hosts.render(serviceInfos))
  }

  def listServices(hostName: String) = Action {
    if("ALL".equalsIgnoreCase(hostName)){
      val serviceInfos = ZookeeperHelper.getInfos()
      Ok(views.html.services(serviceInfos))
    }else{
      val hosts: scala.collection.mutable.Map[String, ListBuffer[ServiceInfo]] = scala.collection.mutable.Map[String, ListBuffer[ServiceInfo]]()
      val serviceInfos = ZookeeperHelper.getInfos()
      hosts += (hostName -> serviceInfos(hostName))
      Ok(views.html.services(hosts.toMap))
    }
  }

  def routeHost = Action {
    Redirect("/")
  }
}

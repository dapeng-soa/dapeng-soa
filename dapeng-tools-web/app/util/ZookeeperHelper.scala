package util

import module.ServiceInfo
import org.apache.zookeeper.{WatchedEvent, Watcher, ZooKeeper}
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set

/**
 * @author Eric on 2016/7/13 11:07
 */
object ZookeeperHelper {
   var zk: ZooKeeper = null
   val ZOOKEEPER_HOST: String = "127.0.0.1:2181"
   val PATH: String = "/soa/runtime/services"
  /**
    * connect to zookeeper
    */
    def connect {
      try {
         zk = new ZooKeeper(ZOOKEEPER_HOST, 15000, new Watcher() {
            def process(e: WatchedEvent) {
               if (e.getState eq Watcher.Event.KeeperState.SyncConnected) {
               }
            }
         })
      }
      catch {
         case e: Exception => {
            e.printStackTrace
         }
      }
   }

  @throws(classOf[Exception])
  private def getServiceNames: List[String] = {
    zk.getChildren(PATH, false).toList
  }

  @throws(classOf[Exception])
  private def getServiceInfo(serviceName: String): List[String] = {
     zk.getChildren(PATH + "/" + serviceName, false).toList
  }

  def getInfos(): Map[String, ListBuffer[ServiceInfo]] = {
    val hosts: scala.collection.mutable.Map[String, ListBuffer[ServiceInfo]] = scala.collection.mutable.Map[String, ListBuffer[ServiceInfo]]()
    try {
      connect
      val services = getServiceNames
      services.foreach(serviceName => {
        val serviceInfos = getServiceInfo(serviceName);
        if (serviceInfos.size > 0) {
          serviceInfos.foreach(serviceInfoL => {
            val host = serviceInfoL.split(":")(0)
            val port = serviceInfoL.split(":")(1)
            val version = serviceInfoL.split(":")(2)
            println(String.format("host : %s , port: %s , version: %s", host, port, version))
            val serviceInfo = new ServiceInfo(serviceName, version, port)
            if (hosts.contains(host)) {
              hosts(host).append(serviceInfo)
            } else {
              hosts += (host -> ListBuffer(serviceInfo))
            }
          })
        }
      })
    }
    catch {
      case e: Exception => {
        System.err.println(e.getMessage)
      }
    } finally {
      try {
        zk.close
      }
      catch {
        case e1: Exception => {
          System.err.println(e1.getMessage)
        }
      }
    }
    hosts.toMap
  }

  def main(args: Array[String]) {
    val hosts = getInfos()
    hosts.map(e =>{
          val (k,v) = e
          v.foreach(serviceInfo =>{
            println(String.format("serviceName: %s, serviceVersion:%s, servicePort:%s",
              serviceInfo.serviceName,serviceInfo.version,serviceInfo.port))
          })
      }
    )
  }
}

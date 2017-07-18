package com.qihoo.rpc

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import scala.collection.mutable
import scala.concurrent.duration._


/**
 * Created by zhaozhuohui on 2017/7/18.
 */
class Master(val host:String, val port:String) extends Actor {

  println("constructor invoked")
  //保存worker注册的信息, workerId -> WorkerInfo
  val idToWorker = new mutable.HashMap[String, WorkerInfo]
  //WorkerInfo,可以进行排序，比如场景：内存大的worker优先用于分配任务
  val workers = new mutable.HashSet[WorkerInfo]()
  //心跳超时检测的间隔
  val CHECK_INTERVAL = 15000

  //receive之前被执行
  override def preStart(): Unit = {
    println("preStart invoked")
    //启动定时器检查哪些worker超时了，并做处理,导入隐式转换
    import context.dispatcher
    context.system.scheduler.schedule(0 millis, CHECK_INTERVAL millis, self, CheckTimeOutWorker)
  }

  // 用于接收消息
  override def receive: Actor.Receive = {
    //接收注册
    case RegisterWorker(id, memory, cores) => {
      //判断是否已经注册过
      if (!idToWorker.contains(id)) {
        //把Worker的信息封装起来保存到内存当中，可以保存到Zookeeper
        val workerInfo = new WorkerInfo(id, memory, cores)
        idToWorker(id) = workerInfo
        workers += workerInfo
        sender ! RegisteredWorker(s"akka.tcp://MasterSystem@${host}:${port}/user/Master")
      }
    }
      //worker发送过来的心跳，id为workerId
    case Heartbeat(id) => {
      if (idToWorker.contains(id)) {
        val workerInfo = idToWorker(id)
        //报活
        val currentTime = System.currentTimeMillis()
        workerInfo.lastHeartbeatTime = currentTime
      }
    }
    case CheckTimeOutWorker => {
      val currentTime = System.currentTimeMillis()
      val toRemove = workers.filter(x => currentTime - x.lastHeartbeatTime > CHECK_INTERVAL)
      for (w <- toRemove) {
        workers -= w
        idToWorker -= w.id
      }
      println(workers.size)
    }
  }
}

object Master {
  def main(args: Array[String]) {

    val host = args(0)
    val port = args(1)
    // 准备配置
    val configStr =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname = "$host"
         |akka.remote.netty.tcp.port = "$port"
       """.stripMargin
    val config = ConfigFactory.parseString(configStr)
    //ActorSystem老大，辅助创建和监控下面的Actor，他是单例的
    val actorSystem = ActorSystem("MasterSystem", config)
    //创建Actor, 起个名字
    val master = actorSystem.actorOf(Props(new Master(host, port)), "Master")//Master主构造器会执行
    actorSystem.awaitTermination()  //让进程等待着, 先别结束
  }
}


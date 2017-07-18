package com.qihoo.rpc

/**
 * Created by zhaozhuohui on 2017/7/18.
 */
class WorkerInfo(val id:String, val memory:Int, val cores:Int) {
  //上一次心跳时间
  var lastHeartbeatTime : Long = _
}

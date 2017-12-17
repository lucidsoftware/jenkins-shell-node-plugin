package com.lucidchart.jenkins.commandnode

import resource.Resource
import scala.concurrent.Promise

object FutureUtil {
  implicit def promiseResource[A] = new Resource[Promise[A]] {
    override def closeAfterException(promise: Promise[A], exception: Throwable) = promise.tryFailure(exception)
    def close(promise: Promise[A]) = ()
  }
}

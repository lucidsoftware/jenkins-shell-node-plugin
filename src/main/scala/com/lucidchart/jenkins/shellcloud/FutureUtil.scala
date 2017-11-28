package com.lucidchart.jenkins.shellcloud

import java.util.concurrent.CompletableFuture
import resource.Resource
import scala.concurrent.Promise
import scala.util.control.NonFatal

object FutureUtil {
  implicit def promiseResource[A] = new Resource[Promise[A]] {
    override def closeAfterException(promise: Promise[A], exception: Throwable) = promise.tryFailure(exception)
    def close(promise: Promise[A]) = ()
  }

  def completeWith[A](future: CompletableFuture[A], value: => A): Unit =
    try future.complete(value)
    catch {
      case e: Throwable =>
        future.completeExceptionally(e)
        if (!NonFatal(e)) {
          throw e
        }
    }
}

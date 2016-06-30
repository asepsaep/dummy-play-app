package utils

import javax.inject.{Inject, Singleton}

import play.api.{Environment, Mode}

@Singleton
class AppMode @Inject() (environment: Environment) {
  def isProd = environment.mode == Mode.Prod
  def isDev = environment.mode == Mode.Dev
  def isTest = environment.mode == Mode.Test
}

import javax.inject._

import play.api._
import play.api.http.DefaultHttpFilters
import filters.LoggingFilter


@Singleton
class Filters @Inject() (
  env: Environment,
  loggingFilter: LoggingFilter) extends DefaultHttpFilters(loggingFilter)


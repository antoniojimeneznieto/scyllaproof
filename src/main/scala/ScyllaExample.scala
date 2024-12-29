import cats.effect._
import cats.implicits._
import cats.effect.implicits._
import io.getquill._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object CassandraCatsEffectExample {
  lazy val ctx = new CassandraAsyncContext(SnakeCase, "catalog")

  import ctx._

  final case class MutantData(
      firstName: String,
      lastName: String,
      address: String,
      pictureLocation: String
  )

  def insertMutant(mutant: MutantData): IO[Unit] =
    IO.fromFuture(IO(ctx.run(query[MutantData].insertValue(lift(mutant))))).void

  // Fetch all mutants
  def fetchMutants: IO[List[MutantData]] =
    IO.fromFuture:
      IO:
        val query1 = quote {
          query[MutantData].filter(md => md.firstName == "Bob" && md.lastName == "Loblaw").take(1)
        }
        val query2 = quote {
          query[MutantData].filter(md => md.firstName == "Jim" && md.lastName == "Jeffries").take(1)
        }

        val fetches = List(ctx.run(query1), ctx.run(query2))

        Future.sequence(fetches).map(_.flatten)

}

import CassandraCatsEffectExample.MutantData
object ScyllaMain extends IOApp.Simple:
  override def run: IO[Unit] =
    val app = CassandraCatsEffectExample

    def randomMutantData: MutantData =
      val firstNames = List("Roberto", "Juan", "Alvaro", "Jose", "Antonio", "Carlos", "Javier", "Pedro", "Pablo", "Francisco", "Manuel")
      val lastNames = List("Jimenez", "Aguilera", "Arturo", "Canovas", "Garcia", "Gonzalez", "Hernandez", "Lopez", "Martinez", "Perez")
      val addresses = List("1313 Mockingbird Lane", "1211 Hollywood Lane", "Xavier's School", "Xavier's School", "1202 Coffman Lane")
      val pictureLocations = List("http://facebook.com/bobloblaw", "http://facebook.com/jeffries", "http://facebook.com/jeangrey", "http://facebook.com/logan")

      val random = new scala.util.Random
      val randomString = random.alphanumeric.take(10).mkString

      MutantData(randomString, randomString, addresses(random.nextInt(addresses.length)), pictureLocations(random.nextInt(pictureLocations.length)))

    def randomMutants: List[MutantData] = (1 to 1000000).toList.map(_ => randomMutantData)

    def app1 = 
      for
        // Prepare
        _         <- app.fetchMutants.flatMap(mutants => IO(println(s"Initial mutants: $mutants")))

        // Insert
        startTimeInsert <- IO(System.currentTimeMillis())
        fiber         <- randomMutants.parTraverseN(100)(app.insertMutant).flatMap(_ => IO.println(s"Time taken to insert: ${System.currentTimeMillis() - startTimeInsert} ms")).start

        // Fetch
        // startTime <- IO(System.currentTimeMillis())
        // _         <- (1 to 100000).toList.parTraverseN(100)(i => app.fetchMutants)
        // endTime   <- IO(System.currentTimeMillis())
        // _         <- IO(println(s"Time taken to fetch: ${endTime - startTime} ms"))
        
        _ <- fiber.joinWithNever
      yield ()

    app1.replicateA(10).void

import com.typesafe.config.ConfigFactory

object DebugConfig extends App {
  val config = ConfigFactory.load()
  println(config.getConfig("catalog"))
}

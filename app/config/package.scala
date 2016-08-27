import com.amazonaws.regions.Regions
import play.api.Configuration

package object config {
  object Guice {
    final val DynamoRepository = "Repository"
  }

  object DynamoDB {
    /**
      * DynamoDB settings depend on application.conf
      * To properly configure the DynamoDB client:
      * - for local usage: endpoint + aws keys + table name
      * - for running on AWS with proper role access on the machine: region + table name
      * @param config Play Configuration object used to access application.conf
      */
    case class Settings(config: Configuration) {
      val endpoint = config.getString("dynamodb.endpoint")
      val awsAccessKeyId = config.getString("dynamodb.aws-access-key-id")
      val awsSecretAccessKey = config.getString("dynamodb.aws-secret-access-key")
      val region = config.getString("dynamodb.region").map(Regions.fromName)
      val tableName = config.getString("dynamodb.table-name")
    }
  }
}

rootProject.name = "ces"

include("server")
include("worker")
include("tests")
include("infrastructure")
include("infrastructure:minio")
findProject(":infrastructure:minio")?.name = "minio"
include("infrastructure:rabbitmq")
findProject(":infrastructure:rabbitmq")?.name = "rabbitmq"
include("infrastructure:rabbitmq")
findProject(":infrastructure:rabbitmq")?.name = "rabbitmq"
include("infrastructure:docker")
findProject(":infrastructure:docker")?.name = "docker"
include("domain")

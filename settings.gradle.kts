rootProject.name = "ces"

include("domain")
include("server")
include("worker")
include("infrastructure")
include("infrastructure:database")
include("infrastructure:rabbitmq")
include("infrastructure:docker")
include("infrastructure:minio")
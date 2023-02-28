rootProject.name = "ces"

include("domain")
include("server")
include("worker")
include("infrastructure")
include("infrastructure:minio")
include("infrastructure:rabbitmq")
include("infrastructure:docker")
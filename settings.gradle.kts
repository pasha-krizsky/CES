rootProject.name = "ces"

include("domain")
include("server")
include("worker")
include("tests")
include("infrastructure")
include("infrastructure:minio")
include("infrastructure:rabbitmq")
include("infrastructure:docker")
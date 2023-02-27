FROM mono:6.12.0.182-slim

RUN apt-get update -y; exit 0
RUN apt-get upgrade -y
RUN apt-get install -y binutils curl mono-devel ca-certificates-mono fsharp mono-vbnc nuget referenceassemblies-pcl \
  && rm -rf /var/lib/apt/lists/* /tmp/*

WORKDIR /home/runner
COPY entrypoint.sh /home/runner

RUN chmod +x entrypoint.sh
ENTRYPOINT ["/home/runner/entrypoint.sh"]
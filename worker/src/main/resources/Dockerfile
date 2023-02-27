FROM mono:6.12.0.182-slim

RUN apt-get update \
  && apt-get install -y binutils curl mono-devel ca-certificates-mono fsharp mono-vbnc nuget referenceassemblies-pcl \
  && rm -rf /var/lib/apt/lists/* /tmp/*

RUN useradd -ms /bin/bash runner
WORKDIR /home/runner
COPY entrypoint.sh /home/runner

USER runner
ENTRYPOINT ["/home/runner/entrypoint.sh"]
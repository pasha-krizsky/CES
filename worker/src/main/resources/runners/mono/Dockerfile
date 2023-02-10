FROM mono:6.12.0.182-slim

RUN apt-get update \
  && apt-get install -y binutils curl mono-devel ca-certificates-mono fsharp mono-vbnc nuget referenceassemblies-pcl \
  && rm -rf /var/lib/apt/lists/* /tmp/*

RUN useradd -ms /bin/bash newuser
WORKDIR /home/newuser
COPY ./file.sh /home/newuser

USER newuser
ENTRYPOINT ["/home/newuser/file.sh"]
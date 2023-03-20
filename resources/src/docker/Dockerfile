FROM      java:8u45
MAINTAINER Corbel Team @ bq <corbel-dev@bq.com>

EXPOSE ${port}
EXPOSE ${port.admin}

ADD run.sh /
ADD ${project.name}-${project.version}-bin.tar.gz /
ADD logback.groovy /${project.name}/etc/logback.groovy

# ----------- Install Imagemagick ---- #
RUN bash
RUN apt update || echo "error"
RUN apt install --force-yes imagemagick
# -------------------------------------------------- #


ENTRYPOINT ["/bin/sh", "/run.sh"]

CMD ["server"]
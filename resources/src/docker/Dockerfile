FROM      java:8u45
MAINTAINER Corbel Team @ bq <corbel-dev@bq.com>

EXPOSE ${port}
EXPOSE ${port.admin}

ADD run.sh /
ADD ${project.name}-${project.version}-bin.tar.gz /
ADD logback.groovy /${project.name}/etc/logback.groovy

# ----------- Install Imagemagick ---- #
RUN bash
RUN apt-get update --allow-unauthenticated || echo "error"
RUN apt-get install -y --allow-unauthenticated imagemagick || echo "error"
# -------------------------------------------------- #


ENTRYPOINT ["/bin/sh", "/run.sh"]

CMD ["server"]

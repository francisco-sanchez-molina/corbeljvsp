find ${project.name}/plugins -name "*.jar" -type f -exec cp '{}' ${project.name}/plugins ';'
cd /
if [ "$SCRIPTS_MODE" = true ] ; then
    find scripts -regex '.*/${project.name}/.*\.groovy' -type f -exec /${project.name}/bin/${project.name} cli '{}' ';'
else
    /${project.name}/bin/${project.name} $@
fi
#Plantilla de application.properties. NO editar application.properties a mano:
#se genera con generate-config.sh a partir de variables de ambiente.
#Variables: BN_WORKSPACE_ROOT, BN_WIN_USER, SOA_HOSTNAME

#ruta de los workspaces de OSB y SOA
osb.dir=${BN_WORKSPACE_ROOT}/BNCR_OSB_WORKSPACE/
soa.dir=${BN_WORKSPACE_ROOT}/BNCR_SOA_WORKSPACE/

#ruta de almacenamiento de JARs de OSB
dest.jar.dir=C:/Users/${BN_WIN_USER}/Documents/OSB_Resources/

#Clave de acceso a servidor linux
server.key=C:/Users/${BN_WIN_USER}/Documents/id_rsa


server.host=${SOA_HOSTNAME}
server.port=22
server.user=oracle
server.remote.path=/u01/scripts/installBN/deploys/

#directorio remoto donde estan installOSB.sh e installSOA.sh
server.script.dir=/u01/scripts/bin/

#OPCIONAL. Archivo known_hosts para verificar la huella del servidor SSH.
#Si no se define NO se verifica (StrictHostKeyChecking=no). Para activarla, registrar
#el host una vez (ssh-keyscan <host> >> <ruta>) y descomentar:
#server.known.hosts=C:/Users/${BN_WIN_USER}/.ssh/known_hosts

package com.soin.pipeline;

import java.util.List;

public class PipelineContext {

    private String host;
    private int port;
    private String scriptDir;
    private String knownHosts;
    private String username;
    private String privateKey;

    private String localFile;
    private String remoteFile;

    private String osbPath;
    private String soaPath;

    private String archivo;

    private boolean isOSB = false;
    private boolean isProject = false;
    private boolean isSOA = false;
    private boolean compile = true;
    private boolean importUpdload = true;
    private boolean create = false;
    private boolean rest = false;

    private int dvmNumber = 0;

    private List<String> gitFiles;

    public int getDvmNumber(){
        return dvmNumber;
    }

    public void setDvmNumber(int dvmNumber){
        this.dvmNumber=dvmNumber;
    }

    public boolean isImport() {
        return importUpdload;
    }

    public void setImport(boolean importUpdload) {
        this.importUpdload = importUpdload;
    }

    public boolean isCompile() {
        return compile;
    }

    public void setCompile(boolean compile) {
        this.compile = compile;
    }

    public List<String> getGitFiles() {
        return gitFiles;
    }

    public void setGitFiles(List<String> gitFiles) {
        this.gitFiles = gitFiles;
    }

    public String getArchivo() {
        return archivo;
    }

    public String getArchivoWithExtension() {
        return archivo + ".jar";
    }

    public String getArchivoSOA() {
        return "sca_" + archivo + ".jar";
    }

    public void setArchivo(String archivo) {
        this.archivo = archivo;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /** Directorio remoto donde viven installOSB.sh e installSOA.sh (siempre termina en "/"). */
    public String getScriptDir() {
        return scriptDir;
    }

    public void setScriptDir(String scriptDir) {
        if (!scriptDir.endsWith("/")) {
            scriptDir += "/";
        }
        this.scriptDir = scriptDir;
    }

    /** Ruta del known_hosts para verificar el servidor SSH; null = no se verifica. */
    public String getKnownHosts() {
        return knownHosts;
    }

    public void setKnownHosts(String knownHosts) {
        this.knownHosts = knownHosts;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getLocalFile() {
        return localFile;
    }

    public void setLocalFile(String localFile) {
        if (!localFile.endsWith("/")) {
            localFile += "/";
        }
        this.localFile = localFile;
    }

    public String getRemoteFile() {
        return remoteFile;
    }

    public void setRemoteFile(String remoteFile) {
        if (!remoteFile.endsWith("/")) {
            remoteFile += "/";
        }
        this.remoteFile = remoteFile;
    }

    public String getOsbPath() {
        return osbPath;
    }

    public void setOsbPath(String osbPath) {
        if (!osbPath.endsWith("/")) {
            osbPath += "/";
        }
        this.osbPath = osbPath;
    }

    public String getSoaPath() {
        return soaPath;
    }

    public void setSoaPath(String soaPath) {
        if (!soaPath.endsWith("/")) {
            soaPath += "/";
        }
        this.soaPath = soaPath;
    }

    public boolean isOSB() {
        return isOSB;
    }

    public void setOSB(boolean isOSB) {
        this.isOSB = isOSB;
    }

    public boolean isProject() {
        return isProject;
    }

    public void setProject(boolean isProject) {
        this.isProject = isProject;
    }

    public boolean isSOA() {
        return isSOA;
    }

    public void setSOA(boolean isSOA) {
        this.isSOA = isSOA;
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isRest() {
        return rest;
    }

    public void setRest(boolean rest) {
        this.rest = rest;
    }

    // getters / setters
}
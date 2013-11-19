/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.Selectors;
import org.codehaus.jettison.json.JSONArray;
import org.jboss.resteasy.annotations.cache.Cache;
import org.obiba.core.util.StreamUtil;
import org.obiba.opal.core.runtime.OpalRuntime;
import org.obiba.opal.core.runtime.security.support.OpalPermissions;
import org.obiba.opal.web.model.Opal;
import org.obiba.opal.web.model.Opal.AclAction;
import org.obiba.opal.web.security.AuthorizationInterceptor;
import org.obiba.opal.web.ws.security.AuthenticatedByCookie;
import org.obiba.opal.web.ws.security.NoAuthorization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Path("/files")
public class FilesResource {

  private static final Logger log = LoggerFactory.getLogger(FilesResource.class);

  private OpalRuntime opalRuntime;

  private final MimetypesFileTypeMap mimeTypes = new MimetypesFileTypeMap();

  @Value("${org.obiba.opal.charset.default}")
  private String defaultCharset;

  @Autowired
  public void setOpalRuntime(OpalRuntime opalRuntime) {
    this.opalRuntime = opalRuntime;
  }

  @GET
  @Path("/_meta")
  @NoAuthorization
  public Response getFileSystemRootDetails() throws FileSystemException {
    return getFileDetails("/");
  }

  @GET
  @Path("/_meta/{path:.*}")
  @NoAuthorization
  public Response getFileDetails(@PathParam("path") String path) throws FileSystemException {
    FileObject file = resolveFileInFileSystem(path);
    if(file.exists()) {
      return file.getType() == FileType.FILE ? getFileDetails(file) : getFolderDetails(file);
    }
    return getPathNotExistResponse(path);
  }

  @GET
  @Path("/")
  @AuthenticatedByCookie
  public Response getFileSystemRoot() throws IOException {
    return getFile("/", null);
  }

  @GET
  @Path("/{path:.*}")
  @AuthenticatedByCookie
  public Response getFile(@PathParam("path") String path, @QueryParam("file") List<String> children)
      throws IOException {
    FileObject file = resolveFileInFileSystem(path);
    if(file.exists()) {
      return file.getType() == FileType.FILE ? getFile(file) : getFolder(file, children);
    }
    return getPathNotExistResponse(path);
  }

  /**
   * Copy or move a file to the current folder.
   *
   * @param path
   * @param action 'copy' (default) or 'move'
   * @param file
   * @return
   * @throws IOException
   */
  @PUT
  @Path("/{path:.*}")
  @AuthenticatedByCookie
  public Response updateFile(@PathParam("path") String destinationPath,
      @QueryParam("action") @DefaultValue("copy") String action, @QueryParam("file") List<String> sourcesPath)
      throws IOException {

    // destination check
    FileObject destinationFile = resolveFileInFileSystem(destinationPath);
    if(!destinationFile.exists()) getPathNotExistResponse(destinationPath);
    if(!destinationFile.isWriteable())
      return Response.status(Status.FORBIDDEN).entity("Destination file is not writable: " + destinationPath).build();

    // sources check
    if(sourcesPath == null || sourcesPath.isEmpty())
      return Response.status(Status.BAD_REQUEST).entity("Source file is missing").build();

    // filter actions: copy, move
    if("move".equals(action.toLowerCase())) {
      return moveTo(destinationFile, sourcesPath);
    }

    if("copy".equals(action.toLowerCase())) {
      return copyFrom(destinationFile, sourcesPath);
    }

    return Response.status(Status.BAD_REQUEST).entity("Unexpected file action: " + action).build();
  }

  private Response moveTo(FileObject destinationFolder, Iterable<String> sourcesPath) throws IOException {
    // destination check
    String destinationPath = destinationFolder.getName().getPath();
    if(destinationFolder.getType() != FileType.FOLDER)
      return Response.status(Status.BAD_REQUEST).entity("Destination must be a folder: " + destinationPath).build();

    // sources check
    for(String sourcePath : sourcesPath) {
      FileObject sourceFile = resolveFileInFileSystem(sourcePath);
      if(!sourceFile.exists()) getPathNotExistResponse(sourcePath);
      if(!sourceFile.isReadable())
        return Response.status(Status.FORBIDDEN).entity("Source file is not readable: " + sourcePath).build();
      if(!sourceFile.isWriteable())
        return Response.status(Status.FORBIDDEN).entity("Source file cannot be moved: " + sourcePath).build();
    }

    // do action
    for(String sourcePath : sourcesPath) {
      FileObject sourceFile = resolveFileInFileSystem(sourcePath);
      FileObject destinationFile = resolveFileInFileSystem(destinationPath + "/" + sourceFile.getName().getBaseName());
      sourceFile.moveTo(destinationFile);
    }

    return Response.ok().build();
  }

  private Response copyFrom(FileObject destinationFolder, Iterable<String> sourcesPath) throws IOException {
    // destination check
    String destinationPath = destinationFolder.getName().getPath();
    if(destinationFolder.getType() != FileType.FOLDER)
      return Response.status(Status.BAD_REQUEST).entity("Destination must be a folder: " + destinationPath).build();

    // sources check
    for(String sourcePath : sourcesPath) {
      FileObject sourceFile = resolveFileInFileSystem(sourcePath);
      if(!sourceFile.exists()) getPathNotExistResponse(sourcePath);
      if(!sourceFile.isReadable())
        return Response.status(Status.FORBIDDEN).entity("Source file is not readable: " + sourcePath).build();
    }

    // do action
    for(String sourcePath : sourcesPath) {
      FileObject sourceFile = resolveFileInFileSystem(sourcePath);
      FileObject destinationFile = resolveFileInFileSystem(destinationPath + "/" + sourceFile.getName().getBaseName());
      FileSelector selector = sourceFile.getType() == FileType.FOLDER ? Selectors.SELECT_ALL : Selectors.SELECT_SELF;
      destinationFile.copyFrom(sourceFile, selector);
    }

    return Response.ok().build();
  }

  @POST
  @Path("/")
  @Consumes("multipart/form-data")
  @Produces("text/html")
  @AuthenticatedByCookie
  public Response uploadFile(@Context UriInfo uriInfo, @Context HttpServletRequest request)
      throws FileSystemException, FileUploadException {
    return uploadFile("/", uriInfo, request);
  }

  // The POST method is required here to be compatible with Html forms which do not support the PUT method.
  @POST
  @Path("/{path:.*}")
  @Consumes("multipart/form-data")
  @Produces("text/html")
  @AuthenticatedByCookie
  public Response uploadFile(@PathParam("path") String path, @Context UriInfo uriInfo,
      @Context HttpServletRequest request) throws FileSystemException, FileUploadException {

    String folderPath = getPathOfFileToWrite(path);
    FileObject folder = resolveFileInFileSystem(folderPath);

    if(folder == null || !folder.exists()) {
      return getPathNotExistResponse(path);
    }
    if(folder.getType() != FileType.FOLDER) {
      return Response.status(Status.FORBIDDEN).entity("Not a folder: " + path).build();
    }

    FileItem uploadedFile = getUploadedFile(request);
    if(uploadedFile == null) {
      return Response.status(Status.BAD_REQUEST)
          .entity("No file has been submitted. Please make sure that you are submitting a file with your request.")
          .build();
    }

    return doUploadFile(folderPath, folder, uploadedFile, uriInfo);
  }

  private Response doUploadFile(String folderPath, FileObject folder, FileItem uploadedFile, UriInfo uriInfo)
      throws FileSystemException {
    String fileName = uploadedFile.getName();
    FileObject file = folder.resolveFile(fileName);
    boolean overwrite = file.exists();

    writeUploadedFileToFileSystem(uploadedFile, file);

    log.info("The following file was uploaded to Opal file system : {}", file.getURL());

    if(overwrite) {
      return Response.ok().build();
    } else {
      URI fileUri = uriInfo.getBaseUriBuilder().path(FilesResource.class).path(folderPath).path(fileName).build();
      return Response.created(fileUri)//
          .header(AuthorizationInterceptor.ALT_PERMISSIONS, new OpalPermissions(fileUri, AclAction.FILES_ALL))//
          .build();
    }
  }

  @POST
  @Path("/")
  @Consumes("text/plain")
  public Response createFolder(String folderName, @Context UriInfo uriInfo) throws FileSystemException {
    return createFolder("/", folderName, uriInfo);
  }

  @POST
  @Path("/{path:.*}")
  @Consumes("text/plain")
  public Response createFolder(@PathParam("path") String path, String folderName, @Context UriInfo uriInfo)
      throws FileSystemException {
    if(folderName == null || folderName.trim().isEmpty()) return Response.status(Status.BAD_REQUEST).build();

    String folderPath = getPathOfFileToWrite(path);
    FileObject folder = resolveFileInFileSystem(folderPath);
    Response folderResponse = validateFolder(folder, path);
    if(folderResponse != null) return folderResponse;

    FileObject file = folder.resolveFile(folderName);
    Response fileResponse = validateFile(file);
    if(fileResponse != null) return fileResponse;

    try {
      file.createFolder();
      Opal.FileDto dto = getBaseFolderBuilder(file).build();
      URI folderUri = uriInfo.getBaseUriBuilder().path(FilesResource.class).path(folderPath).path(folderName).build();
      return Response.created(folderUri)//
          .header(AuthorizationInterceptor.ALT_PERMISSIONS, new OpalPermissions(folderUri, AclAction.FILES_ALL))//
          .entity(dto).build();
    } catch(FileSystemException couldNotCreateTheFolder) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("cannotCreatefolderUnexpectedError").build();
    }
  }

  @Nullable
  private Response validateFolder(FileObject folder, String path) throws FileSystemException {
    if(folder == null || !folder.exists()) {
      return getPathNotExistResponse(path);
    }
    if(folder.getType() != FileType.FOLDER) {
      return Response.status(Status.FORBIDDEN).entity("Not a folder: " + path).build();
    }
    return null;
  }

  @Nullable
  private Response validateFile(FileObject file) throws FileSystemException {
    // Folder or file already exist at specified path.
    if(file.exists()) {
      return Response.status(Status.FORBIDDEN).entity("cannotCreateFolderPathAlreadyExist").build();
    }

    // Parent folder is read-only.
    if(!file.getParent().isWriteable()) {
      return Response.status(Status.FORBIDDEN).entity("cannotCreateFolderParentIsReadOnly").build();
    }

    return null;
  }

  @DELETE
  @Path("/{path:.*}")
  public Response deleteFile(@PathParam("path") String path) throws FileSystemException {
    FileObject file = resolveFileInFileSystem(path);

    // File or folder does not exist.
    if(!file.exists()) {
      return getPathNotExistResponse(path);
    }

    // Read-only file or folder.
    if(!file.isWriteable()) {
      return Response.status(Status.FORBIDDEN).entity("cannotDeleteReadOnlyFile").build();
    }

    try {
      if(file.getType() == FileType.FOLDER) {
        deleteFolder(file);
      } else {
        file.delete();
      }
      return Response.ok("The following file or folder has been deleted : " + path).build();
    } catch(FileSystemException couldNotDeleteFile) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("couldNotDeleteFileError").build();
    }
  }

  //
  // charsets
  //

  @GET
  @Cache
  @Path("/charsets/available")
  @NoAuthorization
  public Response getAvailableCharsets() {
    SortedMap<String, Charset> charsets = Charset.availableCharsets();
    List<String> names = new ArrayList<String>();
    for(Charset charSet : charsets.values()) {
      names.add(charSet.name());
      names.addAll(charSet.aliases());
    }
    return Response.ok(new JSONArray(names).toString()).build();
  }

  @GET
  @Cache
  @Path("/charsets/default")
  @NoAuthorization
  public Response getDefaultCharset() {
    return Response.ok(new JSONArray(Arrays.asList(new String[] { defaultCharset })).toString()).build();
  }

  //
  // private methods
  //

  FileObject resolveFileInFileSystem(String path) throws FileSystemException {
    return opalRuntime.getFileSystem().getRoot().resolveFile(path);
  }

  private Response getFile(FileObject file) {
    File localFile = opalRuntime.getFileSystem().getLocalFile(file);
    String mimeType = mimeTypes.getContentType(localFile);

    return Response.ok(localFile, mimeType)
        .header("Content-Disposition", getContentDispositionOfAttachment(localFile.getName())).build();
  }

  private Response getFolder(FileObject folder, Collection<String> children) throws IOException {
    SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
    String folderName = folder.getName().getBaseName();
    File compressedFolder = new File(System.getProperty("java.io.tmpdir"),
        ("".equals(folderName) ? "filesystem" : folderName) + "_" +
            dateTimeFormatter.format(System.currentTimeMillis()) + ".zip");
    compressedFolder.deleteOnExit();
    String mimeType = mimeTypes.getContentType(compressedFolder);

    compressFolder(compressedFolder, folder, children);

    return Response.ok(compressedFolder, mimeType)
        .header("Content-Disposition", getContentDispositionOfAttachment(compressedFolder.getName())).build();
  }

  private Response getFileDetails(FileObject file) throws FileSystemException {
    Opal.FileDto.Builder fileBuilder;

    fileBuilder = Opal.FileDto.newBuilder();
    fileBuilder.setName(file.getName().getBaseName()).setPath(file.getName().getPath());
    fileBuilder.setType(file.getType() == FileType.FILE ? Opal.FileDto.FileType.FILE : Opal.FileDto.FileType.FOLDER);
    fileBuilder.setReadable(file.isReadable()).setWritable(file.isWriteable());

    // Set size on files only, not folders.
    if(file.getType() == FileType.FILE) {
      fileBuilder.setSize(file.getContent().getSize());
    }

    fileBuilder.setLastModifiedTime(file.getContent().getLastModifiedTime());

    return Response.ok(fileBuilder.build()).build();
  }

  private Response getFolderDetails(FileObject folder) throws FileSystemException {
    // Create a FileDto representing the folder identified by the path.
    Opal.FileDto.Builder folderBuilder = getBaseFolderBuilder(folder);

    // Create FileDtos for each file & folder in the folder corresponding to the path.
    if(folder.isReadable()) {
      addChildren(folderBuilder, folder, 2);
    }

    return Response.ok(folderBuilder.build()).build();

  }

  private Opal.FileDto.Builder getBaseFolderBuilder(FileObject folder) throws FileSystemException {
    Opal.FileDto.Builder fileBuilder = Opal.FileDto.newBuilder();
    String folderName = folder.getName().getBaseName();
    fileBuilder.setName("".equals(folderName) ? "root" : folderName).setType(Opal.FileDto.FileType.FOLDER)
        .setPath(folder.getName().getPath());
    fileBuilder.setLastModifiedTime(folder.getContent().getLastModifiedTime());
    fileBuilder.setReadable(folder.isReadable()).setWritable(folder.isWriteable());
    return fileBuilder;
  }

  private void addChildren(Opal.FileDto.Builder folderBuilder, FileObject parentFolder, int level)
      throws FileSystemException {
    Opal.FileDto.Builder fileBuilder;

    // Get the children for the current folder (list of files & folders).
    List<FileObject> children = Arrays.asList(parentFolder.getChildren());

    Collections.sort(children, new Comparator<FileObject>() {

      @Override
      public int compare(FileObject arg0, FileObject arg1) {
        return arg0.getName().compareTo(arg1.getName());
      }
    });

    // Loop through all children.
    for(FileObject child : children) {
      // Build a FileDto representing the child.
      fileBuilder = Opal.FileDto.newBuilder();
      fileBuilder.setName(child.getName().getBaseName()).setPath(child.getName().getPath());
      fileBuilder.setType(child.getType() == FileType.FILE ? Opal.FileDto.FileType.FILE : Opal.FileDto.FileType.FOLDER);
      fileBuilder.setReadable(child.isReadable()).setWritable(child.isWriteable());

      // Set size on files only, not folders.
      if(child.getType() == FileType.FILE) {
        fileBuilder.setSize(child.getContent().getSize());
      }

      fileBuilder.setLastModifiedTime(child.getContent().getLastModifiedTime());

      if(child.getType().hasChildren() && child.getChildren().length > 0 && level - 1 > 0 && child.isReadable()) {
        addChildren(fileBuilder, child, level - 1);
      }

      // Add the current child to the parent FileDto (folder).
      folderBuilder.addChildren(fileBuilder.build());
    }
  }

  private String getPathOfFileToWrite(String path) {
    return path.startsWith("/tmp/") ? "/tmp/" + UUID.randomUUID().toString() + ".tmp" : path;
  }

  private Response getPathNotExistResponse(String path) {
    return Response.status(Status.NOT_FOUND).entity("The path specified does not exist: " + path).build();
  }

  /**
   * Returns the first {@code FileItem} that is represents a file upload field. If no such field exists, this method
   * returns null
   *
   * @param request
   * @return
   * @throws FileUploadException
   */
  FileItem getUploadedFile(HttpServletRequest request) throws FileUploadException {
    FileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    for(FileItem fileItem : upload.parseRequest(request)) {
      if(!fileItem.isFormField()) {
        return fileItem;
      }
    }

    return null;
  }

  private void writeUploadedFileToFileSystem(FileItem uploadedFile, FileObject fileToWriteTo) {
    OutputStream localFileStream = null;
    InputStream uploadedFileStream = null;
    try {

      // OPAL-919: We need to wrap the OutputStream returned by commons-vfs into another OutputStream
      // to force a call to flush() on every call to write() in order to prevent the system from running out of memory
      // when copying large files.
      localFileStream = new BufferedOutputStream(fileToWriteTo.getContent().getOutputStream()) {
        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
          flush();
          super.write(b, off, len);
        }

      };
      uploadedFileStream = uploadedFile.getInputStream();
      StreamUtil.copy(uploadedFileStream, localFileStream);
    } catch(IOException couldNotWriteUploadedFile) {
      throw new RuntimeException("Could not write uploaded file to Opal file system", couldNotWriteUploadedFile);
    } finally {
      StreamUtil.silentSafeClose(localFileStream);
      StreamUtil.silentSafeClose(uploadedFileStream);
    }

  }

  private void compressFolder(File compressedFile, FileObject folder, Collection<String> children) throws IOException {
    ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(compressedFile));
    addFolder(folder, outputStream, children);
    outputStream.close();
  }

  private void addFolder(FileObject folder, ZipOutputStream outputStream, Collection<String> children)
      throws IOException {
    addFolder(folder.getParent().getName().getPath(), folder, outputStream, children);
  }

  private void addFolder(String basePath, FileObject folder, ZipOutputStream outputStream, Collection<String> children)
      throws IOException {
    int baseLength = "/".equals(basePath) ? 1 : basePath.length() + 1;

    // Add the folder.
    outputStream.putNextEntry(new ZipEntry(folder.getName().getPath().substring(baseLength) + "/"));

    // Add its children files and subfolders.
    FileObject[] files = folder.getChildren();
    for(FileObject file : files) {
      if(children == null || children.isEmpty() || children.contains(file.getName().getBaseName())) {
        // only add files for which download is authorized
        if(file.isReadable()) {
          if(file.getType() == FileType.FOLDER) {
            addFolder(basePath, file, outputStream, null);
          } else {
            outputStream.putNextEntry(new ZipEntry(file.getName().getPath().substring(baseLength)));
            FileInputStream inputStream = new FileInputStream(opalRuntime.getFileSystem().getLocalFile(file));
            StreamUtil.copy(inputStream, outputStream);
            outputStream.closeEntry();
            StreamUtil.silentSafeClose(inputStream);
          }
        }
      }
    }
  }

  /**
   * Delete writable folder and sub-folders.
   *
   * @param folder
   * @throws FileSystemException
   */
  private void deleteFolder(FileObject folder) throws FileSystemException {
    if(!folder.isWriteable()) return;

    FileObject[] files = folder.getChildren();
    for(FileObject file : files) {
      if(file.getType() == FileType.FOLDER) {
        deleteFolder(file);
      } else if(file.isWriteable()) {
        file.delete();
      }
    }
    if(folder.getChildren().length == 0) {
      folder.delete();
    }
  }

  private String getContentDispositionOfAttachment(String fileName) {
    return "attachment; filename=\"" + fileName + "\"";
  }

}

package org.sugarj.driver;

import static org.sugarj.driver.Environment.sep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.sugarj.driver.path.AbsolutePath;
import org.sugarj.driver.path.Path;
import org.sugarj.driver.path.RelativePath;

/**
 * Provides methods for doing stuff with files.
 * 
 * @author Sebastian Erdweg <seba at informatik uni-marburg de>
 */
public class FileCommands {

  public final static boolean DO_DELETE = true;

  public final static String TMP_DIR;
  static {
    try {
      TMP_DIR = File.createTempFile("tmp", "").getParent();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static Path newTempFile(String suffix) throws IOException {
    File f =
        File.createTempFile(
            "sugarj",
            suffix == null || suffix.isEmpty() ? suffix : "." + suffix);
    final Path p = new AbsolutePath(f.getAbsolutePath());
    
    return p;
  }

  public static void deleteTempFiles(Path file) throws IOException {
    if (file == null)
      return;
    
    String parent = file.getFile().getParent();
    
    if (parent == null)
      return;
    else if (parent.equals(TMP_DIR))
      delete(file);
    else 
      deleteTempFiles(new AbsolutePath(parent));
  }
  
  public static void delete(Path file) throws IOException {
    if (file == null)
      return;
    
    if (file.getFile().listFiles() != null)
      for (File f : file.getFile().listFiles())
        FileCommands.delete(new AbsolutePath(f.getPath()));
    
    file.getFile().delete();
  }

  public static void copyFile(Path from, Path to) throws IOException {
    FileInputStream fis = new FileInputStream(from.getFile());
    createFile(to);
    FileOutputStream fos = new FileOutputStream(to.getFile());
    copyFile(fis, fos);
    fis.close();
    fos.close();
  }
  
  public static void copyFile(InputStream in, OutputStream out) throws IOException {
    int len;
    byte[] b = new byte[1024];

    while ((len = in.read(b)) > 0)
      out.write(b, 0, len);
  }

  /**
   * Beware: one must not rename SDF files since the filename and
   * the module name needs to coincide. Instead generate a new
   * file which imports the other SDF file.
   * 
   * @param file
   * @param content
   * @throws IOException
   */
  public static void writeToFile(Path file, String content)
      throws IOException {
    FileCommands.createFile(file);
    FileOutputStream fos = new FileOutputStream(file.getFile());
    fos.write(content.getBytes());
    fos.close();
  }

  public static void appendToFile(Path file, String content)
      throws IOException {
    createFile(file);
    FileOutputStream fos = new FileOutputStream(file.getFile(), true);
    fos.write(content.getBytes());
    fos.close();
  }

  // from http://snippets.dzone.com/posts/show/1335
  // Author: http://snippets.dzone.com/user/daph2001
  public static String readFileAsString(Path filePath) throws IOException {
    StringBuilder fileData = new StringBuilder(1000);
    BufferedReader reader = new BufferedReader(new FileReader(filePath.getFile()));
    char[] buf = new char[1024];
    int numRead = 0;
    while ((numRead = reader.read(buf)) != -1)
      fileData.append(buf, 0, numRead);

    reader.close();
    return fileData.toString();
  }

  
  public static String fileName(URL url) {
    return fileName(new AbsolutePath(url.getPath()));
  }
  
  
  public static String fileName(URI uri) {
    return fileName(new AbsolutePath(uri.getPath()));
  }
  
  public static String fileName(Path file_doof) {
    String file = toCygwinPath(file_doof.getAbsolutePath());
    int index = file.lastIndexOf(sep);

    if (index >= 0)
      file = file.substring(index + 1);

    index = file.lastIndexOf(".");

    if (index > 0)
      file = file.substring(0, index);

    return file;
  }

  /**
   * Finds the given file in the given list of paths.
   * 
   * @param filename
   *        relative filename.
   * @param paths
   *        list of possible paths to filename
   * @return full file path to filename or null
   */
  @Deprecated
  public static String findFile(String filename, List<String> paths) {
    return findFile(filename, paths.toArray(new String[] {}));
  }

  /**
   * Finds the given file in the given list of paths.
   * 
   * @param filename
   *        relative filename.
   * @param paths
   *        list of possible paths to filename
   * @return full file path to filename or null
   */
  @Deprecated
  public static String findFile(String filename, String... paths) {
    for (String path : paths) {
      File f = new File(path + sep + filename);
      if (f.exists())
        return f.getAbsolutePath();
    }

    return null;
  }

  public static Path newTempDir() throws IOException {
    final File f = File.createTempFile("SugarJ", "");
    // need to delete the file, but want to reuse the filename
    f.delete();
    f.mkdir();
    final Path p = new AbsolutePath(f.getAbsolutePath());
    
    return p;
  }

  public static void prependToFile(Path file, String head) throws IOException {
    Path tmp = newTempFile("");
    file.getFile().renameTo(tmp.getFile());

    FileInputStream in = new FileInputStream(tmp.getFile());
    FileOutputStream out = new FileOutputStream(file.getFile());

    out.write(head.getBytes());

    int len;
    byte[] b = new byte[1024];

    while ((len = in.read(b)) > 0)
      out.write(b, 0, len);

    in.close();
    out.close();
    delete(tmp);
  }

  public static void createFile(Path file) throws IOException {
    File f = file.getFile();
    if (f.getParentFile().exists() || f.getParentFile().mkdirs())
      f.createNewFile();
  }

  /**
   * Create file with name deduced from hash in dir.
   * 
   * @param dir
   * @param hash
   * @return
   * @throws IOException
   */
  public static Path createFile(Path dir, int hash) throws IOException {
    Path p = new RelativePath(dir, hashFileName("sugarj", hash));
    createFile(p);
    return p;
  }

  public static void createDir(Path dir) throws IOException {
    dir.getFile().mkdirs();
  }

  /**
   * Create directory with name deduced from hash in dir.
   * 
   * @param dir
   * @param hash
   * @return
   * @throws IOException
   */
  public static Path createDir(Path dir, int hash) throws IOException {
    Path p = new RelativePath(dir, hashFileName("SugarJ", hash));
    createDir(p);
    return p;
  }

  /**
   * Ensures that a path is suitable for a cygwin command line.
   */
  public static String toCygwinPath(String filepath) {
    // XXX hacky

    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      filepath = filepath.replace("\\", "/");
      filepath = filepath.replace("/C:/", "/cygdrive/C/");
      filepath = filepath.replace("C:/", "/cygdrive/C/");
    }

    return filepath;
  }

  /**
   * Ensure that a path is suitable for a windows command line
   */
  public static String toWindowsPath(String filepath) {
    // XXX hacky

    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      filepath = filepath.replace("/cygdrive/C", "C:");
      filepath = filepath.replace("/C:", "C:");
      filepath = filepath.replace("/", "\\");
    }

    return filepath;
  }

  /**
   * checks whether f1 was modified after f2.
   * 
   * @return true iff f1 was modified after f2.
   */
  public static boolean isModifiedLater(Path f1, Path f2) {
    return f1.getFile().lastModified() > f2.getFile().lastModified();
  }

  public static boolean exists(Path file) {
    return file.getFile().exists();
  }
  
  public static boolean exists(URI file) {
    return new File(file).exists();
  }

  public static String hashFileName(String prefix, int hash) {
    return prefix + (hash < 0 ? "1" + Math.abs(hash) : "0" + hash);
  }

  public static String hashFileName(String prefix, Object o) {
    return hashFileName(prefix, o.hashCode());
  }

  public static String getExtension(File infile) {
    int i = infile.getName().lastIndexOf('.');
    
    if (i > 0)
      return infile.getName().substring(i+1, infile.getName().length());
    
    return null;
  }
  
  public static String dropExtension(String file) {
    int i = file.lastIndexOf('.');
    
    if (i > 0)
      return file.substring(0, i);
    
    return file;
  }

  public static String getRelativeModulePath(String module) {
    return module.replace(".", sep);
  }

  public static int fileHash(Path file) throws IOException {
    if (exists(file))
      return readFileAsString(file).hashCode();
    
    return 0;
  }
}

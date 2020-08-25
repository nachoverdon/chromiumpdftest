import com.github.kklisura.cdt.launch.ChromeLauncher;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDF;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDFTransferMode;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.types.ChromeTab;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;

record PdfOptions(String url, String header, String footer) {}

public class Main {
  // If the PDF is too large, we might need to increase the buffer to avoid app hangs
  // https://github.com/kklisura/chrome-devtools-java-client#api-hangs-ie-when-printing-pdfs
  // static {
  //   // Set the incoming buffer to 24MB
  //   System.setProperty(
  //       DefaultWebSocketContainerFactory.WEBSOCKET_INCOMING_BUFFER_PROPERTY,
  //       Long.toString((long) DefaultWebSocketContainerFactory.MB * 24));
  // }

  public static void main(String[] args) {
    try (
        FileOutputStream out = new FileOutputStream("./out/file.pdf");
        )
    {
      String url = "https://github.com/nachoverdon";
      String header = FileUtils.readFileToString(new File("./src/main/resources/header.html"), "UTF-8");
      String footer = FileUtils.readFileToString(new File("./src/main/resources/footer.html"), "UTF-8");
      PdfOptions options = new PdfOptions(url, header, footer);

      printPDF(out, options);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void printPDF(OutputStream out, PdfOptions options) {
    // Create chrome launcher.
    final ChromeLauncher launcher = new ChromeLauncher();

    // Launch chrome either as headless (true) - PDF printing is only supported on Chrome headless
    // at the moment
    final ChromeService chromeService = launcher.launch(true);

    // Create empty tab ie about:blank.
    final ChromeTab tab = chromeService.createTab();

    // Get DevTools service to this tab
    final ChromeDevToolsService devToolsService = chromeService.createDevToolsService(tab);

    // Get individual commands
    final Page page = devToolsService.getPage();
    page.enable();

    // Navigate to page
    page.navigate(options.url());

    page.onLoadEventFired(
        loadEventFired -> {
          System.out.println("Printing to PDF...");

          PrintToPDF pdf = page.printToPDF(
              false, true, true, 1.0d, 8.27d, 11.7d, 0.4, 0.4, 0.4,
              0.4, "0", true, options.header(), options.footer(), true,
              PrintToPDFTransferMode.RETURN_AS_BASE_64
          );

          try {
            byte[] pdfBytes = Base64.getDecoder().decode(pdf.getData());

            IOUtils.write(pdfBytes, out);
          } catch (IOException e) {
            e.printStackTrace();
          }

          devToolsService.close();
          System.out.println("Done!");
        });

    devToolsService.waitUntilClosed();
  }

}

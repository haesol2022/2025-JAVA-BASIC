package print;

import model.PurchaseModel;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class ReportGenerator {

    public static void printInvoice(PurchaseModel purchase) {
        try {
            JasperReport report = JasperCompileManager.compileReport("src/print/Invoice.jrxml");

            Map<String, Object> params = new HashMap<>();
            params.put("party", purchase.getPartyName());
            params.put("contact", ""); // 필요 시 DB에서 가져와서 세팅
            params.put("invoiceNo", String.valueOf(purchase.getOrderId()));
            params.put("totalQuantity", String.valueOf(purchase.getTotalQuantity()));
            params.put("totalAmount", String.valueOf(purchase.getTotalAmount()));
            params.put("otherAmount", String.valueOf(purchase.getOtherAmount()));
            params.put("paybleAmount", String.valueOf(purchase.getTotalPaybleAmount()));
            params.put("paidAmount", String.valueOf(purchase.getTotalPaidAmount()));
            params.put("dueAmount", String.valueOf(purchase.getTotalDueAmount()));
            params.put("remarks", ""); // 필요 시 DB에서 가져오기
            params.put("currency", "KRW");
            params.put("taux", "1.0");

            // 현재는 Item 리스트 없이 단건 데이터만 출력
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Collections.emptyList());

            JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);
            JasperViewer.viewReport(print, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

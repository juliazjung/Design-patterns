import java.text.SimpleDateFormat;
import java.util.Date;

public class RegistradorLog {
    private String idNo;
    private SimpleDateFormat formatadorData;

    public RegistradorLog(String idNo) {
        this.idNo = idNo;
        this.formatadorData = new SimpleDateFormat("HH:mm:ss.SSS");
    }

    public synchronized void registrar(String mensagem) {
        String dataFormatada = formatadorData.format(new Date());
        System.out.println("[" + dataFormatada + "][" + idNo + "] " + mensagem);
    }
}
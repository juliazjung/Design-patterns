
public class RegistradorLog {
    private String idNo;

    public RegistradorLog(String idNo) {
        this.idNo = idNo;
    }

    public synchronized void registrar(String mensagem) {
        GerenciadorLog.getInstancia().registrar(idNo, mensagem);
    }
}

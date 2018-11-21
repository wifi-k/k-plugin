package tbcloud.user.api.http.rsp;

import java.math.BigDecimal;

/**
 * @author dzh
 * @date 2018-11-21 14:35
 */
public class ShareSumRsp {

    private BigDecimal sum;
    private BigDecimal balance;
    private BigDecimal yesterday;

    public BigDecimal getSum() {
        return sum;
    }

    public void setSum(BigDecimal sum) {
        this.sum = sum;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getYesterday() {
        return yesterday;
    }

    public void setYesterday(BigDecimal yesterday) {
        this.yesterday = yesterday;
    }
}

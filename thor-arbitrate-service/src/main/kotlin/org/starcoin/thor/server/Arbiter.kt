package org.starcoin.thor.server

import org.starcoin.lightning.client.core.Invoice
import org.starcoin.lightning.client.core.Payment

import java.util.*

class Arbiter {
    private var accounts: Collection<Account> = HashSet()

    fun register(account: Account) {
        if (accounts.contains(account))
            throw RuntimeException("Account has been registed")
        accounts += account
    }

    fun match(accountA: Account, accountB: Account) {
        if (!accounts.contains(accountA) || !accounts.contains(accountB)) {
            throw RuntimeException("Account has not been registed")
        }
        val invoiceA = accountA.addInvoice(Invoice(accountA.preimage.hash(), accountB.amount))
        accountB.sendPayment(Payment(invoiceA.paymentRequest))
        val invoiceB = accountB.addInvoice(Invoice(accountB.preimage.hash(), accountA.amount))
        accountA.sendPayment(Payment(invoiceB.paymentRequest))
    }

    fun challenge(account: Account): String? {
        // TODO: vm check which user win
        if((0..1).random() == 0){
            return account.preimage.hex()
        }
        return null
    }
}
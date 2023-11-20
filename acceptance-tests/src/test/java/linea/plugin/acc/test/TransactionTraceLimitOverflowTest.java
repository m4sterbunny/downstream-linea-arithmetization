/*
 * Copyright Consensys Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package linea.plugin.acc.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.List;

import linea.plugin.acc.test.tests.web3j.generated.SimpleStorage;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.tests.acceptance.dsl.account.Accounts;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Numeric;

public class TransactionTraceLimitOverflowTest extends LineaPluginTestBase {

  private static final BigInteger GAS_LIMIT = DefaultGasProvider.GAS_LIMIT;
  private static final BigInteger VALUE = BigInteger.ZERO;
  private static final BigInteger GAS_PRICE = BigInteger.ONE;

  @Override
  public List<String> getTestCliOptions() {
    return new TestCommandLineOptionsBuilder()
        .set(
            "--plugin-linea-module-limit-file-path=",
            getResourcePath("/txOverflowModuleLimits.json"))
        .build();
  }

  @Test
  public void transactionOverModuleLineCountRemoved() throws Exception {
    final SimpleStorage simpleStorage = deploySimpleStorage();
    final Web3j web3j = minerNode.nodeRequests().eth();
    final String contractAddress = simpleStorage.getContractAddress();
    final Credentials credentials = Credentials.create(Accounts.GENESIS_ACCOUNT_ONE_PRIVATE_KEY);
    final String txData = simpleStorage.add(BigInteger.valueOf(100)).encodeFunctionCall();

    // this tx will not be selected since it goes above the line count limit
    // but selection should go on and select the next one
    final RawTransaction txModuleLineCountTooBig =
        RawTransaction.createTransaction(
            CHAIN_ID,
            BigInteger.valueOf(0),
            GAS_LIMIT,
            contractAddress,
            VALUE,
            txData,
            GAS_PRICE.multiply(BigInteger.TEN),
            GAS_PRICE.multiply(BigInteger.TEN));
    final byte[] signedTxContractInteraction =
        TransactionEncoder.signMessage(
            txModuleLineCountTooBig, Credentials.create(Accounts.GENESIS_ACCOUNT_TWO_PRIVATE_KEY));
    final EthSendTransaction signedTxContractInteractionResp =
        web3j.ethSendRawTransaction(Numeric.toHexString(signedTxContractInteraction)).send();

    // this is under the line count limit and should be selected
    final RawTransaction txTransfer =
        RawTransaction.createTransaction(
            CHAIN_ID,
            BigInteger.valueOf(1),
            GAS_LIMIT,
            Address.ZERO.toHexString(),
            VALUE,
            "",
            GAS_PRICE,
            GAS_PRICE);
    final byte[] signedTxTransfer = TransactionEncoder.signMessage(txTransfer, credentials);
    final EthSendTransaction signedTxTransferResp =
        web3j.ethSendRawTransaction(Numeric.toHexString(signedTxTransfer)).send();

    assertTransactionsMinedInSeparateBlocks(
        web3j, List.of(signedTxTransferResp.getTransactionHash()));

    // assert that tx over line count limit is not confirmed and is removed from the pool
    final EthGetTransactionReceipt receipt =
        web3j.ethGetTransactionReceipt(signedTxContractInteractionResp.getTransactionHash()).send();
    assertThat(receipt.getTransactionReceipt()).isEmpty();
    assertTransactionNotInThePool(signedTxContractInteractionResp.getTransactionHash());
  }
}

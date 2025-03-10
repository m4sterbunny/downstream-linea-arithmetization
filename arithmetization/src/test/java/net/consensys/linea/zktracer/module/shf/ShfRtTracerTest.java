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

package net.consensys.linea.zktracer.module.shf;

import java.util.Random;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import net.consensys.linea.zktracer.opcode.OpCode;
import net.consensys.linea.zktracer.testing.BytecodeCompiler;
import net.consensys.linea.zktracer.testing.BytecodeRunner;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
class ShfRtTracerTest {
  private static final Random RAND = new Random();
  private static final int TEST_REPETITIONS = 4;

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideShiftOperators")
  void testFailingBlockchainBlock(final int opCodeValue) {
    BytecodeRunner.of(
            BytecodeCompiler.newProgram()
                .push(Bytes32.rightPad(Bytes.fromHexString("0x08")))
                .push(Bytes32.fromHexString("0x01"))
                .immediate(opCodeValue)
                .compile())
        .run();
  }

  @Test
  void TestShfResultFailure() {
    BytecodeRunner.of(
            Bytes.fromHexString(
                "7faaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa7fa0000000000000000000000000000000000000000000000000000000000000001d"))
        .run();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideRandomSarArguments")
  void testRandomSar(final Bytes32[] payload) {
    log.info(
        "value: " + payload[0].toShortHexString() + ", shift by: " + payload[1].toShortHexString());

    BytecodeRunner.of(
            BytecodeCompiler.newProgram()
                .push(payload[1])
                .push(payload[0])
                .op(OpCode.SAR)
                .compile())
        .run();
  }

  @Test
  void testTmp() {
    BytecodeRunner.of(
            BytecodeCompiler.newProgram()
                .immediate(Bytes32.fromHexStringLenient("0x54fda4f3c1452c8c58df4fb1e9d6de"))
                .immediate(Bytes32.fromHexStringLenient("0xb5"))
                .op(OpCode.SAR)
                .compile())
        .run();
  }

  private static Stream<Arguments> provideRandomSarArguments() {
    final Arguments[] arguments = new Arguments[TEST_REPETITIONS];

    for (int i = 0; i < TEST_REPETITIONS; i++) {
      final boolean signBit = RAND.nextInt(2) == 1;

      // leave the first byte untouched
      final int k = 1 + RAND.nextInt(31);

      final byte[] randomBytes = new byte[k];
      RAND.nextBytes(randomBytes);

      final byte[] signBytes = new byte[32 - k];
      if (signBit) {
        signBytes[0] = (byte) 0x80; // 0b1000_0000, i.e. sign bit == 1
      }

      final byte[] bytes = concatenateArrays(signBytes, randomBytes);
      byte shiftBy = (byte) RAND.nextInt(256);

      Bytes32[] payload = new Bytes32[2];
      payload[0] = Bytes32.wrap(bytes);
      payload[1] = Bytes32.leftPad(Bytes.of(shiftBy));

      arguments[i] =
          Arguments.of(
              Named.of(
                  "value: "
                      + payload[0].toHexString()
                      + ", shiftBy: "
                      + payload[1].toShortHexString(),
                  payload));
    }

    return Stream.of(arguments);
  }

  public static Stream<Arguments> provideShiftOperators() {
    return Stream.of(
        Arguments.of(Named.of("SAR", OpCode.SAR.getData().value())),
        Arguments.of(Named.of("SHL", OpCode.SHL.getData().value())),
        Arguments.of(Named.of("SHR", OpCode.SHR.getData().value())));
  }

  private static byte[] concatenateArrays(byte[] a, byte[] b) {
    int length = a.length + b.length;
    byte[] result = new byte[length];
    System.arraycopy(a, 0, result, 0, a.length);
    System.arraycopy(b, 0, result, a.length, b.length);

    return result;
  }
}

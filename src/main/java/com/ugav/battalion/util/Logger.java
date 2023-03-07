package com.ugav.battalion.util;

import java.io.PrintStream;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public interface Logger {

	public void dbgln();

	public void dbg(byte s);

	public void dbgln(byte s);

	public void dbg(char s);

	public void dbgln(char s);

	public void dbg(short s);

	public void dbgln(short s);

	public void dbg(int s);

	public void dbgln(int s);

	public void dbg(long s);

	public void dbgln(long s);

	public void dbg(float s);

	public void dbgln(float s);

	public void dbg(double s);

	public void dbgln(double s);

	public void dbg(boolean s);

	public void dbgln(boolean s);

	public void dbg(Object s);

	public void dbgln(Object s);

	public void dbg(Object arg1, Object arg2);

	public void dbg(Object arg1, Object arg2, Object arg3);

	public void dbg(Object arg1, Object arg2, Object arg3, Object arg4);

	public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

	public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

	public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

	public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
			Object arg8);

	public void dbg(Object arg1, Object... args);

	public void dbgln(Object arg1, Object arg2);

	public void dbgln(Object arg1, Object arg2, Object arg3);

	public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4);

	public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

	public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6);

	public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7);

	public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
			Object arg8);

	public void dbgln(Object arg1, Object... args);

	public void dbgf(String s, Object arg1);

	public void dbgf(String s, Object arg1, Object arg2);

	public void dbgf(String s, Object arg1, Object arg2, Object arg3);

	public void dbgf(String s, Object arg1, Object arg2, Object arg3, Object arg4);

	public void dbgf(String s, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5);

	public void dbgf(String s, Object... args);

	static Logger createDefault() {
		return new PrintStreamLogger();
	}

	static class Enabled extends Forward {

		private final BooleanSupplier isEnable;

		public Enabled(Logger logger, BooleanSupplier isEnable) {
			super(logger);
			this.isEnable = Objects.requireNonNull(isEnable);
		}

		@Override
		boolean isDbgEn() {
			return isEnable.getAsBoolean();
		}

	}

	static class PrintStreamLogger implements Logger {

		private PrintStream out;

		public PrintStreamLogger() {
			this(null);
		}

		public PrintStreamLogger(PrintStream printStream) {
			out = printStream != null ? printStream : System.out;
		}

		public void setPrintStream(PrintStream printStream) {
			this.out = printStream;
		}

		@Override
		public void dbgln() {
			out.println();
		}

		@Override
		public void dbg(byte s) {
			out.print(s);
		}

		@Override
		public void dbgln(byte s) {
			out.println(s);
		}

		@Override
		public void dbg(char s) {
			out.print(s);
		}

		@Override
		public void dbgln(char s) {
			out.println(s);
		}

		@Override
		public void dbg(short s) {
			out.print(s);
		}

		@Override
		public void dbgln(short s) {
			out.println(s);
		}

		@Override
		public void dbg(int s) {
			out.print(s);
		}

		@Override
		public void dbgln(int s) {
			out.println(s);
		}

		@Override
		public void dbg(long s) {
			out.print(s);
		}

		@Override
		public void dbgln(long s) {
			out.println(s);
		}

		@Override
		public void dbg(float s) {
			out.print(s);
		}

		@Override
		public void dbgln(float s) {
			out.println(s);
		}

		@Override
		public void dbg(double s) {
			out.print(s);
		}

		@Override
		public void dbgln(double s) {
			out.println(s);
		}

		@Override
		public void dbg(boolean s) {
			out.print(s);
		}

		@Override
		public void dbgln(boolean s) {
			out.println(s);
		}

		@Override
		public void dbg(Object s) {
			out.print(s);
		}

		@Override
		public void dbgln(Object s) {
			out.println(s);
		}

		@Override
		public void dbg(Object arg1, Object arg2) {
			out.print(String.valueOf(arg1) + arg2);
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3) {
			out.print(String.valueOf(arg1) + arg2 + arg3);
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4) {
			out.print(String.valueOf(arg1) + arg2 + arg3 + arg4);
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
			out.print(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5);
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
			out.print(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6);
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
			out.print(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6 + arg7);
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
				Object arg8) {
			out.print(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6 + arg7 + arg8);
		}

		@Override
		public void dbg(Object arg1, Object... args) {
			StringBuilder builder = new StringBuilder();
			builder.append(arg1);
			for (Object arg : args)
				builder.append(arg);
			out.print(builder.toString());
		}

		@Override
		public void dbgln(Object arg1, Object arg2) {
			out.println(String.valueOf(arg1) + arg2);
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3) {
			out.println(String.valueOf(arg1) + arg2 + arg3);
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4) {
			out.println(String.valueOf(arg1) + arg2 + arg3 + arg4);
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
			out.println(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5);
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
			out.println(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6);
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
			out.println(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6 + arg7);
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
				Object arg8) {
			out.println(String.valueOf(arg1) + arg2 + arg3 + arg4 + arg5 + arg6 + arg7 + arg8);
		}

		@Override
		public void dbgln(Object arg1, Object... args) {
			StringBuilder builder = new StringBuilder();
			builder.append(arg1);
			for (Object arg : args)
				builder.append(arg);
			out.println(builder.toString());
		}

		@Override
		public void dbgf(String s, Object arg1) {
			out.format(s, arg1);
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2) {
			out.format(s, arg1, arg2);
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2, Object arg3) {
			out.format(s, arg1, arg2, arg3);
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2, Object arg3, Object arg4) {
			out.format(s, arg1, arg2, arg3, arg4);
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
			out.format(s, arg1, arg2, arg3, arg4, arg5);
		}

		@Override
		public void dbgf(String s, Object... args) {
			out.format(s, args);
		}
	}

	static class Forward implements Logger {

		final Logger logger;

		Forward(Logger logger) {
			this.logger = Objects.requireNonNull(logger);
		}

		boolean isDbgEn() {
			return true;
		}

		void beforeDbg() {
		}

		void afterDbg() {
		}

		@Override
		public void dbgln() {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln();
				afterDbg();
			}
		}

		@Override
		public void dbg(byte s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(s);
				afterDbg();
			}
		}

		@Override
		public void dbgln(byte s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(s);
				afterDbg();
			}
		}

		@Override
		public void dbg(char s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(s);
				afterDbg();
			}
		}

		@Override
		public void dbgln(char s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(s);
				afterDbg();
			}
		}

		@Override
		public void dbg(short s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(s);
				afterDbg();
			}
		}

		@Override
		public void dbgln(short s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(s);
				afterDbg();
			}
		}

		@Override
		public void dbg(int s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(s);
				afterDbg();
			}
		}

		@Override
		public void dbgln(int s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(s);
				afterDbg();
			}
		}

		@Override
		public void dbg(long s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(s);
				afterDbg();
			}
		}

		@Override
		public void dbgln(long s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(s);
				afterDbg();
			}
		}

		@Override
		public void dbg(float s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(s);
				afterDbg();
			}
		}

		@Override
		public void dbgln(float s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(s);
				afterDbg();
			}
		}

		@Override
		public void dbg(double s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(s);
				afterDbg();
			}
		}

		@Override
		public void dbgln(double s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(s);
				afterDbg();
			}
		}

		@Override
		public void dbg(boolean s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(s);
				afterDbg();
			}
		}

		@Override
		public void dbgln(boolean s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(s);
				afterDbg();
			}
		}

		@Override
		public void dbg(Object s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(s);
				afterDbg();
			}
		}

		@Override
		public void dbgln(Object s) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(s);
				afterDbg();
			}
		}

		@Override
		public void dbg(Object arg1, Object arg2) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(arg1, arg2);
				afterDbg();
			}
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(arg1, arg2, arg3);
				afterDbg();
			}
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(arg1, arg2, arg3, arg4);
				afterDbg();
			}
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(arg1, arg2, arg3, arg4, arg5);
				afterDbg();
			}
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(arg1, arg2, arg3, arg4, arg5, arg6);
				afterDbg();
			}
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
				afterDbg();
			}
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
				Object arg8) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
				afterDbg();
			}
		}

		@Override
		public void dbg(Object arg1, Object... args) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbg(arg1, args);
				afterDbg();
			}
		}

		@Override
		public void dbgln(Object arg1, Object arg2) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(arg1, arg2);
				afterDbg();
			}
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(arg1, arg2, arg3);
				afterDbg();
			}
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(arg1, arg2, arg3, arg4);
				afterDbg();
			}
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(arg1, arg2, arg3, arg4, arg5);
				afterDbg();
			}
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(arg1, arg2, arg3, arg4, arg5, arg6);
				afterDbg();
			}
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(arg1, arg2, arg3, arg4, arg5, arg6, arg7);
				afterDbg();
			}
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
				Object arg8) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
				afterDbg();
			}
		}

		@Override
		public void dbgln(Object arg1, Object... args) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgln(arg1, args);
				afterDbg();
			}
		}

		@Override
		public void dbgf(String s, Object arg1) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgf(s, arg1);
				afterDbg();
			}
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgf(s, arg1, arg2);
				afterDbg();
			}
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2, Object arg3) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgf(s, arg1, arg2, arg3);
				afterDbg();
			}
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2, Object arg3, Object arg4) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgf(s, arg1, arg2, arg3, arg4);
				afterDbg();
			}
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgf(s, arg1, arg2, arg3, arg4, arg5);
				afterDbg();
			}
		}

		@Override
		public void dbgf(String s, Object... args) {
			if (isDbgEn()) {
				beforeDbg();
				logger.dbgf(s, args);
				afterDbg();
			}
		}

	}

	static class Null implements Logger {

		public Null() {
		}

		@Override
		public void dbgln() {
		}

		@Override
		public void dbg(byte s) {
		}

		@Override
		public void dbgln(byte s) {
		}

		@Override
		public void dbg(char s) {
		}

		@Override
		public void dbgln(char s) {
		}

		@Override
		public void dbg(short s) {
		}

		@Override
		public void dbgln(short s) {
		}

		@Override
		public void dbg(int s) {
		}

		@Override
		public void dbgln(int s) {
		}

		@Override
		public void dbg(long s) {
		}

		@Override
		public void dbgln(long s) {
		}

		@Override
		public void dbg(float s) {
		}

		@Override
		public void dbgln(float s) {
		}

		@Override
		public void dbg(double s) {
		}

		@Override
		public void dbgln(double s) {
		}

		@Override
		public void dbg(boolean s) {
		}

		@Override
		public void dbgln(boolean s) {
		}

		@Override
		public void dbg(Object s) {
		}

		@Override
		public void dbgln(Object s) {
		}

		@Override
		public void dbg(Object arg1, Object arg2) {
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3) {
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4) {
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
		}

		@Override
		public void dbg(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
				Object arg8) {
		}

		@Override
		public void dbg(Object arg1, Object... args) {
		}

		@Override
		public void dbgln(Object arg1, Object arg2) {
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3) {
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4) {
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6) {
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7) {
		}

		@Override
		public void dbgln(Object arg1, Object arg2, Object arg3, Object arg4, Object arg5, Object arg6, Object arg7,
				Object arg8) {
		}

		@Override
		public void dbgln(Object arg1, Object... args) {
		}

		@Override
		public void dbgf(String s, Object arg1) {
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2) {
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2, Object arg3) {
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2, Object arg3, Object arg4) {
		}

		@Override
		public void dbgf(String s, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		}

		@Override
		public void dbgf(String s, Object... args) {
		}

	}

}
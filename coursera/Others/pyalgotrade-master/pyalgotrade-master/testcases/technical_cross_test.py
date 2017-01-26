# PyAlgoTrade
# 
# Copyright 2011 Gabriel Martin Becedillas Ruiz
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
.. moduleauthor:: Gabriel Martin Becedillas Ruiz <gabriel.becedillas@gmail.com>
"""

import unittest

from pyalgotrade.technical import cross
from pyalgotrade.technical import ma
from pyalgotrade import dataseries

class TestCase(unittest.TestCase):
	def __buildSeqDS(self, values):
		ret = dataseries.SequenceDataSeries()
		for value in values:
			ret.append(value)
		return ret

	def testCrossAboveOnce(self):
		values1 = self.__buildSeqDS([1, 1, 1, 10, 1, 1, 1])
		values2 = self.__buildSeqDS([2, 2, 2,  2, 2, 2, 2])

		# Check every 2 values.
		self.assertEqual(cross.cross_above(values1, values2, 0, 2), 0)
		self.assertEqual(cross.cross_above(values1, values2, 1, 3), 0)
		self.assertEqual(cross.cross_above(values1, values2, 2, 4), 1)
		self.assertEqual(cross.cross_above(values1, values2, 3, 5), 0)
		self.assertEqual(cross.cross_above(values1, values2, 4, 6), 0)
		self.assertEqual(cross.cross_above(values1, values2, 5, 7), 0)

		# Check every 3 values.
		self.assertEqual(cross.cross_above(values1, values2, 0, 3), 0)
		self.assertEqual(cross.cross_above(values1, values2, 1, 4), 1)
		self.assertEqual(cross.cross_above(values1, values2, 2, 5), 1)
		self.assertEqual(cross.cross_above(values1, values2, 3, 6), 0)
		self.assertEqual(cross.cross_above(values1, values2, 4, 7), 0)

		# Check for all values.
		self.assertEqual(cross.cross_above(values1, values2, 0, 7), 1)
		self.assertEqual(cross.cross_above(values1, values2, 0, -1), 1)

	def testCrossAboveMany(self):
		count = 100
		values1 = [-1 if i % 2 == 0 else 1 for i in range(count)]
		values2 = [0 for i in range(count)]

		# Check first value
		self.assertEqual(cross.cross_above(values1, values2, 0, 0), 0)

		# Check every 2 values.
		period = 2
		for i in range(1, count):
			if i % 2 == 0:
				self.assertEqual(cross.cross_above(values1, values2, i - period + 1, i + 1), 0)
			else:
				self.assertEqual(cross.cross_above(values1, values2, i - period + 1, i + 1), 1)

		# Check every 4 values.
		period = 4
		for i in range(3, count):
			if i % 2 == 0:
				self.assertEqual(cross.cross_above(values1, values2, i - period + 1, i + 1), 1)
			else:
				self.assertEqual(cross.cross_above(values1, values2, i - period + 1, i + 1), 2)

		# Check for all values.
		self.assertEqual(cross.cross_above(values1, values2, 0, count), count / 2)

	def testCrossBelowOnce(self):
		values1 = self.__buildSeqDS([2, 2, 2,  2, 2, 2, 2])
		values2 = self.__buildSeqDS([1, 1, 1, 10, 1, 1, 1])

		# Check every 2 values.
		self.assertEqual(cross.cross_below(values1, values2, 0, 2), 0)
		self.assertEqual(cross.cross_below(values1, values2, 1, 3), 0)
		self.assertEqual(cross.cross_below(values1, values2, 2, 4), 1)
		self.assertEqual(cross.cross_below(values1, values2, 3, 5), 0)
		self.assertEqual(cross.cross_below(values1, values2, 4, 6), 0)
		self.assertEqual(cross.cross_below(values1, values2, 5, 7), 0)

		# Check every 3 values.
		self.assertEqual(cross.cross_below(values1, values2, 0, 3), 0)
		self.assertEqual(cross.cross_below(values1, values2, 1, 4), 1)
		self.assertEqual(cross.cross_below(values1, values2, 2, 5), 1)
		self.assertEqual(cross.cross_below(values1, values2, 3, 6), 0)
		self.assertEqual(cross.cross_below(values1, values2, 4, 7), 0)

		# Check for all values.
		self.assertEqual(cross.cross_below(values1, values2, 0, 7), 1)
		self.assertEqual(cross.cross_below(values1, values2, 0, -1), 1)

	def testCrossBelowMany(self):
		count = 100
		values1 = [0 for i in range(count)]
		values2 = [-1 if i % 2 == 0 else 1 for i in range(count)]

		# Check first value
		self.assertEqual(cross.cross_below(values1, values2, 0, 0), 0)

		# Check every 2 values.
		period = 2
		for i in range(1, count):
			if i % 2 == 0:
				self.assertEqual(cross.cross_below(values1, values2, i - period + 1, i + 1), 0)
			else:
				self.assertEqual(cross.cross_below(values1, values2, i - period + 1, i + 1), 1)

		# Check every 4 values.
		period = 4
		for i in range(3, count):
			if i % 2 == 0:
				self.assertEqual(cross.cross_below(values1, values2, i - period + 1, i + 1), 1)
			else:
				self.assertEqual(cross.cross_below(values1, values2, i - period + 1, i + 1), 2)

		# Check for all values.
		self.assertEqual(cross.cross_below(values1, values2, 0, count), count / 2)

	def testCrossAboveWithSMA(self):
		ds1 = dataseries.SequenceDataSeries()
		ds2 = dataseries.SequenceDataSeries()
		sma1 = ma.SMA(ds1, 15)
		sma2 = ma.SMA(ds2, 25)
		for i in range(100):
			ds1.append(i)
			ds2.append(50)
			if i == 58:
				self.assertEqual(cross.cross_above(sma1[:], sma2[:], -2, None), 1)
			else:
				self.assertEqual(cross.cross_above(sma1[:], sma2[:], -2, None), 0)

def getTestCases():
	ret = []

	ret.append(TestCase("testCrossAboveOnce"))
	ret.append(TestCase("testCrossAboveMany"))
	ret.append(TestCase("testCrossBelowOnce"))
	ret.append(TestCase("testCrossBelowMany"))
	ret.append(TestCase("testCrossAboveWithSMA"))

	return ret

'''
(c) 2011, 2012 Georgia Tech Research Corporation
This source code is released under the New BSD license.  Please see
http://wiki.quantsoftware.org/index.php?title=QSTK_License
for license details.

Created on January, 23, 2013

@author: Sourabh Bajaj
@contact: sourabhbajaj@gatech.edu
@summary: Event Profiler Tutorial
'''


import pandas as pd
import numpy as np
import math
import copy
import QSTK.qstkutil.qsdateutil as du
import datetime as dt
import QSTK.qstkutil.DataAccess as da
import QSTK.qstkutil.tsutil as tsu
import QSTK.qstkstudy.EventProfiler as ep
import csv

"""
Accepts a list of symbols along with start and end date
Returns the Event Matrix which is a pandas Datamatrix
Event matrix has the following structure :
    |IBM |GOOG|XOM |MSFT| GS | JP |
(d1)|nan |nan | 1  |nan |nan | 1  |
(d2)|nan | 1  |nan |nan |nan |nan |
(d3)| 1  |nan | 1  |nan | 1  |nan |
(d4)|nan |  1 |nan | 1  |nan |nan |
...................................
...................................
Also, d1 = start date
nan = no information about any event.
1 = status bit(positively confirms the event occurence)
"""


def find_events(ls_symbols, d_data):
    ''' Finding the event dataframe '''
    df_close = d_data['actual_close']
    # ts_market = df_close['SPY']

    print "Finding Events"

    # Creating an empty dataframe
    df_events = copy.deepcopy(df_close)
    df_events = df_events * np.NAN

    # Time stamps for the event range
    ldt_timestamps = df_close.index

    #Erase the existing file
    file = open('orders.csv', 'w')
    writer = csv.writer(file, delimiter=',')
    file.close()

    for s_sym in ls_symbols:
        for i in range(1, len(ldt_timestamps)):
            # Calculating the returns for this timestamp
            f_symprice_today = df_close[s_sym].ix[ldt_timestamps[i]]
            f_symprice_yest = df_close[s_sym].ix[ldt_timestamps[i - 1]]
            # f_marketprice_today = ts_market.ix[ldt_timestamps[i]]
            # f_marketprice_yest = ts_market.ix[ldt_timestamps[i - 1]]
            # f_symreturn_today = (f_symprice_today / f_symprice_yest) - 1
            # f_marketreturn_today = (f_marketprice_today / f_marketprice_yest) - 1

            # Event is found if the symbol is down more then 3% while the
            # market is up more then 2%
            if f_symprice_today < 6.0 and f_symprice_yest >= 6.0:
                buy_data = [ldt_timestamps[i].year, ldt_timestamps[i].month, ldt_timestamps[i].day, s_sym, 'Buy', 100] 
                print buy_data
                write_csv(buy_data)
                sell_date = du.getNYSEoffset(ldt_timestamps[i],5)
                sell_data = [sell_date.year, sell_date.month, sell_date.day, s_sym, 'Sell', 100] 
                print sell_data
                write_csv(sell_data)
                #df_events[s_sym].ix[ldt_timestamps[i]] = 1

    return df_events

def write_csv(data):
    global values_matrix
    
    file = open('orders.csv', 'ab')
    writer = csv.writer(file, delimiter=',')

    writer.writerow(data)
    file.close()

if __name__ == '__main__':
    dt_start = dt.datetime(2008, 1, 1)
    dt_end = dt.datetime(2009, 12, 31)
    ldt_timestamps = du.getNYSEdays(dt_start, dt_end, dt.timedelta(hours=16))

    dataobj = da.DataAccess('Yahoo')
    ls_symbols = dataobj.get_symbols_from_list('sp5002012')
    # ls_symbols = dataobj.get_symbols_from_list('sp5002008')
    ls_symbols.append('SPY')
    print "test1"
    ls_keys = ['open', 'high', 'low', 'close', 'volume', 'actual_close']
    ldf_data = dataobj.get_data(ldt_timestamps, ls_symbols, ls_keys)
    print "test2"

    d_data = dict(zip(ls_keys, ldf_data))

    print "test3"
    for s_key in ls_keys:
        d_data[s_key] = d_data[s_key].fillna(method='ffill')
        d_data[s_key] = d_data[s_key].fillna(method='bfill')
        d_data[s_key] = d_data[s_key].fillna(1.0)

    df_events = find_events(ls_symbols, d_data)
    print "Creating Study"
    ep.eventprofiler(df_events, d_data, i_lookback=20, i_lookforward=20,
                s_filename='MyEventStudy-q2-2012.pdf', b_market_neutral=True, b_errorbars=True,
                s_market_sym='SPY')